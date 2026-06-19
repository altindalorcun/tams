import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, BookOpen } from "lucide-react";
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
import {
  getDepartments, createDepartment, updateDepartment, deleteDepartment,
  getDepartmentCourses, addCourseToDepartment, removeCourseFromDepartment, getCourses,
} from "@/api/ruleApi";
import type { Department, CreateDepartmentRequest, DepartmentCourse } from "@/types";

const schema = z.object({
  name: z.string().min(1, "İsim zorunludur"),
  code: z.string().min(1, "Kod zorunludur"),
});
type FormValues = z.infer<typeof schema>;

interface DeptDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initial?: Department;
  onSave: (data: CreateDepartmentRequest) => Promise<void>;
}

function DeptDialog({ open, onOpenChange, initial, onSave }: DeptDialogProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: initial ? { name: initial.name, code: initial.code } : { name: "", code: "" },
  });

  async function onSubmit(values: FormValues) {
    await onSave(values);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm shadow-xl">
        <DialogHeader>
          <DialogTitle>{initial ? "Bölümü Düzenle" : "Yeni Bölüm"}</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField control={form.control} name="name" render={({ field }) => (
              <FormItem>
                <FormLabel>Bölüm Adı</FormLabel>
                <FormControl><Input placeholder="Bilgisayar Mühendisliği" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="code" render={({ field }) => (
              <FormItem>
                <FormLabel>Kod</FormLabel>
                <FormControl><Input placeholder="BBM" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
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

interface DeptCoursesPoolDialogProps {
  departmentId: string;
  departmentName: string;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}

function DeptCoursesPoolDialog({ departmentId, departmentName, open, onOpenChange }: DeptCoursesPoolDialogProps) {
  const qc = useQueryClient();

  const { data: allCourses = [] } = useQuery({ queryKey: ["courses"], queryFn: getCourses, enabled: open });
  const { data: deptCourses = [], isLoading } = useQuery({
    queryKey: ["department-courses", departmentId],
    queryFn: () => getDepartmentCourses(departmentId),
    enabled: open,
  });

  const assignedIds = new Set(deptCourses.map((c: DepartmentCourse) => c.courseId));

  const addMut = useMutation({
    mutationFn: (courseId: string) => addCourseToDepartment(departmentId, courseId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["department-courses", departmentId] }); toast.success("Ders havuza eklendi."); },
    onError: () => toast.error("Ders eklenemedi."),
  });

  const removeMut = useMutation({
    mutationFn: (courseId: string) => removeCourseFromDepartment(departmentId, courseId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["department-courses", departmentId] }); toast.success("Ders havuzdan çıkarıldı."); },
    onError: () => toast.error("Ders çıkarılamadı."),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl shadow-xl">
        <DialogHeader>
          <DialogTitle>{departmentName} — Ders Havuzu</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground -mt-1">
          Bu bölümün ders havuzuna ders ekleyin veya çıkarın. Havuzdaki dersler mezuniyet kategorilerine atanabilir.
        </p>
        <div className="grid grid-cols-2 gap-4 pt-2 min-h-[240px]">
          <div>
            <p className="text-sm font-medium mb-2">Havuzdaki Dersler</p>
            {isLoading ? (
              <Skeleton className="h-32 w-full" />
            ) : (
              <div className="space-y-1 max-h-64 overflow-y-auto rounded-md border p-2">
                {deptCourses.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center py-4">Henüz ders yok</p>
                )}
                {deptCourses.map((c: DepartmentCourse) => (
                  <div key={c.courseId} className="flex items-center justify-between rounded px-2 py-1 hover:bg-muted/50">
                    <div>
                      <span className="font-mono text-xs text-muted-foreground">{c.courseCode}</span>
                      <span className="ml-1 text-sm">{c.courseName}</span>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6 text-destructive"
                      onClick={() => removeMut.mutate(c.courseId)}
                      aria-label="Havuzdan çıkar"
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div>
            <p className="text-sm font-medium mb-2">Eklenebilir Dersler</p>
            <div className="space-y-1 max-h-64 overflow-y-auto rounded-md border p-2">
              {allCourses.filter((c) => !assignedIds.has(c.id)).length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">Eklenecek ders yok</p>
              )}
              {allCourses.filter((c) => !assignedIds.has(c.id)).map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded px-2 py-1 hover:bg-muted/50">
                  <div>
                    <span className="font-mono text-xs text-muted-foreground">{c.courseCode}</span>
                    <span className="ml-1 text-sm">{c.courseName}</span>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-6 text-xs"
                    onClick={() => addMut.mutate(c.id)}
                    aria-label="Havuza ekle"
                  >
                    <Plus className="h-3 w-3 mr-1" />
                    Ekle
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Kapat</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Admin tab: manage university departments.
 */
export function DepartmentsTab() {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Department | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<Department | undefined>();
  const [poolTarget, setPoolTarget] = useState<Department | undefined>();

  const { data: departments, isLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  const createMut = useMutation({
    mutationFn: createDepartment,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["departments"] }); toast.success("Bölüm eklendi."); },
    onError: () => toast.error("Bölüm eklenemedi."),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateDepartmentRequest }) => updateDepartment(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["departments"] }); toast.success("Bölüm güncellendi."); },
    onError: () => toast.error("Güncelleme başarısız."),
  });

  const deleteMut = useMutation({
    mutationFn: deleteDepartment,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["departments"] }); toast.success("Bölüm silindi."); setDeleteTarget(undefined); },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  function openCreate() { setEditTarget(undefined); setDialogOpen(true); }
  function openEdit(d: Department) { setEditTarget(d); setDialogOpen(true); }

  async function handleSave(data: CreateDepartmentRequest) {
    if (editTarget) {
      await updateMut.mutateAsync({ id: editTarget.id, data });
    } else {
      await createMut.mutateAsync(data);
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Bölümler</h2>
        <Button size="sm" onClick={openCreate} className="transition-colors duration-150">
          <Plus className="mr-1 h-4 w-4" />
          Yeni Bölüm
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
        </div>
      ) : (
        <div className="rounded-lg border shadow-sm overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Bölüm Adı</TableHead>
                <TableHead>Kod</TableHead>
                <TableHead className="w-32 text-right">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {departments?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground py-8">
                    Henüz bölüm eklenmemiş.
                  </TableCell>
                </TableRow>
              )}
              {departments?.map((d) => (
                <TableRow key={d.id} className="hover:bg-muted/50 transition-colors duration-150">
                  <TableCell className="font-medium">{d.name}</TableCell>
                  <TableCell className="text-muted-foreground">{d.code}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="icon" onClick={() => setPoolTarget(d)} aria-label="Ders havuzu" title="Ders Havuzu">
                        <BookOpen className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => openEdit(d)} aria-label="Düzenle">
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(d)} aria-label="Sil" className="text-destructive hover:text-destructive">
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

      <DeptDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initial={editTarget}
        onSave={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => { if (!v) setDeleteTarget(undefined); }}
        title="Bölümü sil"
        description={`"${deleteTarget?.name}" bölümünü kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />

      {poolTarget && (
        <DeptCoursesPoolDialog
          departmentId={poolTarget.id}
          departmentName={poolTarget.name}
          open={!!poolTarget}
          onOpenChange={(v) => { if (!v) setPoolTarget(undefined); }}
        />
      )}
    </section>
  );
}
