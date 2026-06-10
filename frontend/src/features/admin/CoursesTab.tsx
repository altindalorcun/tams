import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { getCourses, createCourse, updateCourse, deleteCourse } from "@/api/ruleApi";
import type { Course, CreateCourseRequest } from "@/types";

const schema = z.object({
  courseCode: z.string().min(1, "Ders kodu zorunludur"),
  name: z.string().min(1, "Ders adı zorunludur"),
  credits: z.coerce.number().min(0, "Kredi 0 veya üzeri olmalıdır"),
  ects: z.coerce.number().min(0, "AKTS 0 veya üzeri olmalıdır"),
});
type FormValues = z.infer<typeof schema>;

interface CourseDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initial?: Course;
  onSave: (data: CreateCourseRequest) => Promise<void>;
}

function CourseDialog({ open, onOpenChange, initial, onSave }: CourseDialogProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: initial
      ? { courseCode: initial.courseCode, name: initial.name, credits: initial.credits, ects: initial.ects }
      : { courseCode: "", name: "", credits: 0, ects: 0 },
  });

  async function onSubmit(values: FormValues) {
    await onSave(values);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm shadow-xl">
        <DialogHeader>
          <DialogTitle>{initial ? "Dersi Düzenle" : "Yeni Ders"}</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField control={form.control} name="courseCode" render={({ field }) => (
              <FormItem>
                <FormLabel>Ders Kodu</FormLabel>
                <FormControl><Input placeholder="BBM101" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="name" render={({ field }) => (
              <FormItem>
                <FormLabel>Ders Adı</FormLabel>
                <FormControl><Input placeholder="Algoritma ve Programlamaya Giriş" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="credits" render={({ field }) => (
                <FormItem>
                  <FormLabel>Kredi</FormLabel>
                  <FormControl><Input type="number" min={0} {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="ects" render={({ field }) => (
                <FormItem>
                  <FormLabel>AKTS</FormLabel>
                  <FormControl><Input type="number" min={0} {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
            </div>
            <DialogFooter className="pt-2">
              <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>İptal</Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {initial ? "Kaydet" : "Ekle"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Admin tab: manage the global course catalog.
 */
export function CoursesTab() {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Course | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<Course | undefined>();

  const { data: courses, isLoading } = useQuery({
    queryKey: ["courses"],
    queryFn: getCourses,
  });

  const createMut = useMutation({
    mutationFn: createCourse,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["courses"] }); toast.success("Ders eklendi."); },
    onError: () => toast.error("Ders eklenemedi."),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateCourseRequest }) => updateCourse(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["courses"] }); toast.success("Ders güncellendi."); },
    onError: () => toast.error("Güncelleme başarısız."),
  });

  const deleteMut = useMutation({
    mutationFn: deleteCourse,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["courses"] }); toast.success("Ders silindi."); setDeleteTarget(undefined); },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  async function handleSave(data: CreateCourseRequest) {
    if (editTarget) {
      await updateMut.mutateAsync({ id: editTarget.id, data });
    } else {
      await createMut.mutateAsync(data);
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Ders Kataloğu</h2>
        <Button size="sm" onClick={() => { setEditTarget(undefined); setDialogOpen(true); }} className="transition-colors duration-150">
          <Plus className="mr-1 h-4 w-4" />
          Yeni Ders
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </div>
      ) : (
        <div className="rounded-lg border shadow-sm overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Ders Kodu</TableHead>
                <TableHead>Ders Adı</TableHead>
                <TableHead className="text-right">Kredi</TableHead>
                <TableHead className="text-right">AKTS</TableHead>
                <TableHead className="w-24 text-right">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {courses?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    Ders kataloğu boş.
                  </TableCell>
                </TableRow>
              )}
              {courses?.map((c) => (
                <TableRow key={c.id} className="hover:bg-muted/50 transition-colors duration-150">
                  <TableCell className="font-mono text-sm">{c.courseCode}</TableCell>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell className="text-right tabular-nums">{c.credits}</TableCell>
                  <TableCell className="text-right tabular-nums">{c.ects}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="icon" onClick={() => { setEditTarget(c); setDialogOpen(true); }} aria-label="Düzenle">
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(c)} aria-label="Sil" className="text-destructive hover:text-destructive">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <CourseDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initial={editTarget}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => { if (!v) setDeleteTarget(undefined); }}
        title="Dersi sil"
        description={`"${deleteTarget?.courseCode} — ${deleteTarget?.name}" dersini katalogdan silmek istediğinize emin misiniz?`}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />
    </section>
  );
}
