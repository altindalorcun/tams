import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, ChevronDown, ChevronRight, BookOpen } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import {
  getDepartments, getCategories, createCategory, updateCategory, deleteCategory,
  getCategoryCourses, getCourses, addCourseToCategory, removeCourseFromCategory,
} from "@/api/ruleApi";
import type { Category, CreateCategoryRequest, CategoryCourse } from "@/types";

const catSchema = z.object({
  name: z.string().min(1, "Kategori adı zorunludur"),
  minCourseCount: z.coerce.number().min(1, "En az 1 ders gereklidir"),
  minCredit: z.coerce.number().optional(),
  minEcts: z.coerce.number().optional(),
});
type CatFormValues = z.infer<typeof catSchema>;

interface CatDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initial?: Category;
  onSave: (data: CreateCategoryRequest) => Promise<void>;
}

function CatDialog({ open, onOpenChange, initial, onSave }: CatDialogProps) {
  const form = useForm<CatFormValues>({
    resolver: zodResolver(catSchema),
    values: initial
      ? { name: initial.name, minCourseCount: initial.minCourseCount, minCredit: initial.minCredit, minEcts: initial.minEcts }
      : { name: "", minCourseCount: 1, minCredit: undefined, minEcts: undefined },
  });

  async function onSubmit(v: CatFormValues) {
    await onSave({ name: v.name, minCourseCount: v.minCourseCount, minCredit: v.minCredit || undefined, minEcts: v.minEcts || undefined });
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm shadow-xl">
        <DialogHeader><DialogTitle>{initial ? "Kategoriyi Düzenle" : "Yeni Kategori"}</DialogTitle></DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField control={form.control} name="name" render={({ field }) => (
              <FormItem><FormLabel>Kategori Adı</FormLabel><FormControl><Input placeholder="Zorunlu Dersler" {...field} /></FormControl><FormMessage /></FormItem>
            )} />
            <FormField control={form.control} name="minCourseCount" render={({ field }) => (
              <FormItem><FormLabel>Minimum Ders Sayısı</FormLabel><FormControl><Input type="number" min={1} {...field} /></FormControl><FormMessage /></FormItem>
            )} />
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="minCredit" render={({ field }) => (
                <FormItem><FormLabel>Min. Kredi <span className="text-muted-foreground text-xs">(isteğe bağlı)</span></FormLabel><FormControl><Input type="number" min={0} placeholder="—" {...field} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="minEcts" render={({ field }) => (
                <FormItem><FormLabel>Min. AKTS <span className="text-muted-foreground text-xs">(isteğe bağlı)</span></FormLabel><FormControl><Input type="number" min={0} placeholder="—" {...field} /></FormControl><FormMessage /></FormItem>
              )} />
            </div>
            <DialogFooter className="pt-2">
              <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>İptal</Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>{initial ? "Kaydet" : "Ekle"}</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

interface CoursesPoolDialogProps {
  catId: string;
  catName: string;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}

function CoursesPoolDialog({ catId, catName, open, onOpenChange }: CoursesPoolDialogProps) {
  const qc = useQueryClient();

  const { data: allCourses = [] } = useQuery({ queryKey: ["courses"], queryFn: getCourses, enabled: open });
  const { data: catCourses = [], isLoading } = useQuery({
    queryKey: ["category-courses", catId],
    queryFn: () => getCategoryCourses(catId),
    enabled: open,
  });

  const assignedIds = new Set(catCourses.map((c: CategoryCourse) => c.courseId));

  const addMut = useMutation({
    mutationFn: ({ courseId, isMandatory }: { courseId: string; isMandatory: boolean }) =>
      addCourseToCategory(catId, courseId, isMandatory),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["category-courses", catId] }); toast.success("Ders eklendi."); },
    onError: () => toast.error("Ders eklenemedi."),
  });

  const removeMut = useMutation({
    mutationFn: (courseId: string) => removeCourseFromCategory(catId, courseId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["category-courses", catId] }); toast.success("Ders çıkarıldı."); },
    onError: () => toast.error("Ders çıkarılamadı."),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl shadow-xl">
        <DialogHeader><DialogTitle>{catName} — Ders Havuzu</DialogTitle></DialogHeader>
        <div className="grid grid-cols-2 gap-4 pt-2 min-h-[240px]">
          {/* Left: assigned courses */}
          <div>
            <p className="text-sm font-medium mb-2">Kategorideki Dersler</p>
            {isLoading ? <Skeleton className="h-32 w-full" /> : (
              <div className="space-y-1 max-h-64 overflow-y-auto rounded-md border p-2">
                {catCourses.length === 0 && <p className="text-sm text-muted-foreground text-center py-4">Ders yok</p>}
                {catCourses.map((c: CategoryCourse) => (
                  <div key={c.courseId} className="flex items-center justify-between rounded px-2 py-1 hover:bg-muted/50">
                    <div>
                      <span className="font-mono text-xs text-muted-foreground">{c.courseCode}</span>
                      <span className="ml-1 text-sm">{c.courseName}</span>
                      {c.isMandatory && <Badge variant="outline" className="ml-1 text-xs">Zorunlu</Badge>}
                    </div>
                    <Button variant="ghost" size="icon" className="h-6 w-6 text-destructive" onClick={() => removeMut.mutate(c.courseId)}>
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
          {/* Right: available courses to add */}
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
                  <div className="flex gap-1">
                    <Button size="sm" variant="outline" className="h-6 text-xs" onClick={() => addMut.mutate({ courseId: c.id, isMandatory: false })}>Seçmeli</Button>
                    <Button size="sm" className="h-6 text-xs" onClick={() => addMut.mutate({ courseId: c.id, isMandatory: true })}>Zorunlu</Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
        <DialogFooter><Button variant="outline" onClick={() => onOpenChange(false)}>Kapat</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

interface DeptCategoryListProps {
  departmentId: string;
  departmentName: string;
}

function DeptCategoryList({ departmentId, departmentName }: DeptCategoryListProps) {
  const qc = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Category | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<Category | undefined>();
  const [poolCat, setPoolCat] = useState<Category | undefined>();

  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories", departmentId],
    queryFn: () => getCategories(departmentId),
    enabled: expanded,
  });

  const createMut = useMutation({
    mutationFn: (data: CreateCategoryRequest) => createCategory(departmentId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["categories", departmentId] }); toast.success("Kategori eklendi."); },
    onError: () => toast.error("Kategori eklenemedi."),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateCategoryRequest }) => updateCategory(departmentId, id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["categories", departmentId] }); toast.success("Kategori güncellendi."); },
    onError: () => toast.error("Güncelleme başarısız."),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteCategory(departmentId, id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["categories", departmentId] }); toast.success("Kategori silindi."); setDeleteTarget(undefined); },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  async function handleSave(data: CreateCategoryRequest) {
    if (editTarget) await updateMut.mutateAsync({ id: editTarget.id, data });
    else await createMut.mutateAsync(data);
  }

  return (
    <Card className="shadow-sm">
      <CardHeader className="p-4 cursor-pointer" onClick={() => setExpanded((v) => !v)}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {expanded ? <ChevronDown className="h-4 w-4 text-muted-foreground" /> : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
            <span className="font-medium">{departmentName}</span>
          </div>
          {expanded && (
            <Button size="sm" variant="outline" onClick={(e) => { e.stopPropagation(); setEditTarget(undefined); setDialogOpen(true); }}>
              <Plus className="mr-1 h-4 w-4" />Kategori Ekle
            </Button>
          )}
        </div>
      </CardHeader>
      {expanded && (
        <CardContent className="p-0 pt-0">
          {isLoading ? (
            <div className="px-4 pb-4 space-y-2"><Skeleton className="h-9 w-full" /><Skeleton className="h-9 w-full" /></div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Kategori Adı</TableHead>
                  <TableHead className="text-right">Min. Ders</TableHead>
                  <TableHead className="text-right">Min. Kredi</TableHead>
                  <TableHead className="text-right">Min. AKTS</TableHead>
                  <TableHead className="w-32 text-right">İşlemler</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {categories?.length === 0 && (
                  <TableRow><TableCell colSpan={5} className="text-center text-muted-foreground py-6">Bu bölüme ait kategori yok.</TableCell></TableRow>
                )}
                {categories?.map((cat) => (
                  <TableRow key={cat.id} className="hover:bg-muted/50 transition-colors duration-150">
                    <TableCell className="font-medium">{cat.name}</TableCell>
                    <TableCell className="text-right tabular-nums">{cat.minCourseCount}</TableCell>
                    <TableCell className="text-right tabular-nums text-muted-foreground">{cat.minCredit ?? "—"}</TableCell>
                    <TableCell className="text-right tabular-nums text-muted-foreground">{cat.minEcts ?? "—"}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" onClick={() => setPoolCat(cat)} aria-label="Ders havuzu" title="Ders havuzu">
                          <BookOpen className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" onClick={() => { setEditTarget(cat); setDialogOpen(true); }} aria-label="Düzenle">
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(cat)} aria-label="Sil" className="text-destructive hover:text-destructive">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      )}

      <CatDialog open={dialogOpen} onOpenChange={setDialogOpen} initial={editTarget} onSave={handleSave} />
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => { if (!v) setDeleteTarget(undefined); }}
        title="Kategoriyi sil"
        description={`"${deleteTarget?.name}" kategorisini silmek istediğinize emin misiniz?`}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />
      {poolCat && (
        <CoursesPoolDialog catId={poolCat.id} catName={poolCat.name} open={!!poolCat} onOpenChange={(v) => { if (!v) setPoolCat(undefined); }} />
      )}
    </Card>
  );
}

/**
 * Admin tab: manage graduation categories per department.
 */
export function CategoriesTab() {
  const { data: departments, isLoading } = useQuery({ queryKey: ["departments"], queryFn: getDepartments });

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold">Mezuniyet Kategorileri</h2>
      <p className="text-sm text-muted-foreground">
        Her bölüm için mezuniyet kategorilerini ve ders havuzlarını yönetin.
      </p>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
        </div>
      ) : departments?.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-8">Önce Bölümler sekmesinden bir bölüm ekleyin.</p>
      ) : (
        <div className="space-y-3">
          {departments?.map((d) => (
            <DeptCategoryList key={d.id} departmentId={d.id} departmentName={d.name} />
          ))}
        </div>
      )}
    </section>
  );
}
