import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, ChevronDown, ChevronRight, BookOpen, Filter } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage, FormDescription } from "@/components/ui/form";
import { Separator } from "@/components/ui/separator";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import {
  getDepartments, getCategories, createCategory, updateCategory, deleteCategory,
  getCategoryCourses, getCourses, addCourseToCategory, removeCourseFromCategory,
  addPrefixLimit, deletePrefixLimit,
} from "@/api/ruleApi";
import type { Category, CreateCategoryRequest, CategoryCourse, PrefixLimit } from "@/types";

const catSchema = z.object({
  name: z.string().min(1, "Kategori adı zorunludur"),
  minCourseCount: z.coerce.number().min(1, "En az 1 ders gereklidir"),
  minCredit: z.coerce.number().optional(),
  minEcts: z.coerce.number().optional(),
  appliesFromYear: z.coerce.number().int().positive().optional().or(z.literal("")),
  appliesToYear: z.coerce.number().int().positive().optional().or(z.literal("")),
  conditionCourseCodes: z.string().optional(),
  minCourseCountIfMet: z.coerce.number().int().min(0).optional().or(z.literal("")),
  minEctsIfMet: z.coerce.number().min(0).optional().or(z.literal("")),
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
      ? {
          name: initial.name,
          minCourseCount: initial.minCourseCount,
          minCredit: initial.minCredit,
          minEcts: initial.minEcts,
          appliesFromYear: initial.appliesFromYear ?? ("" as const),
          appliesToYear: initial.appliesToYear ?? ("" as const),
          conditionCourseCodes: initial.conditionCourseCodes?.join(", ") ?? "",
          minCourseCountIfMet: initial.minCourseCountIfMet ?? ("" as const),
          minEctsIfMet: initial.minEctsIfMet ?? ("" as const),
        }
      : {
          name: "", minCourseCount: 1, minCredit: undefined, minEcts: undefined,
          appliesFromYear: "" as const, appliesToYear: "" as const,
          conditionCourseCodes: "", minCourseCountIfMet: "" as const, minEctsIfMet: "" as const,
        },
  });

  async function onSubmit(v: CatFormValues) {
    const parsedCodes = v.conditionCourseCodes
      ? v.conditionCourseCodes.split(",").map((c) => c.trim().toUpperCase()).filter(Boolean)
      : [];
    await onSave({
      name: v.name,
      minCourseCount: v.minCourseCount,
      minCredit: v.minCredit || undefined,
      minEcts: v.minEcts || undefined,
      appliesFromYear: v.appliesFromYear === "" || v.appliesFromYear === undefined ? null : Number(v.appliesFromYear),
      appliesToYear: v.appliesToYear === "" || v.appliesToYear === undefined ? null : Number(v.appliesToYear),
      conditionCourseCodes: parsedCodes.length > 0 ? parsedCodes : null,
      minCourseCountIfMet: v.minCourseCountIfMet === "" || v.minCourseCountIfMet === undefined ? null : Number(v.minCourseCountIfMet),
      minEctsIfMet: v.minEctsIfMet === "" || v.minEctsIfMet === undefined ? null : Number(v.minEctsIfMet),
    });
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md shadow-xl max-h-[90vh] overflow-y-auto">
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
            <Separator />
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Kohort Aralığı</p>
            <FormDescription className="text-xs -mt-2">Bu kategori yalnızca belirtilen kayıt yılı aralığındaki öğrencilere uygulanır. Boş bırakırsanız sınır yoktur.</FormDescription>
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="appliesFromYear" render={({ field }) => (
                <FormItem><FormLabel>Başlangıç Yılı</FormLabel><FormControl><Input type="number" placeholder="örn. 2015" {...field} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="appliesToYear" render={({ field }) => (
                <FormItem><FormLabel>Bitiş Yılı</FormLabel><FormControl><Input type="number" placeholder="örn. 2025" {...field} /></FormControl><FormMessage /></FormItem>
              )} />
            </div>
            <Separator />
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Koşullu Eşikler</p>
            <FormDescription className="text-xs -mt-2">
              Öğrenci bu ders kodlarından herhangi birini geçmişse alternatif eşikler uygulanır.
              Boş bırakılırsa koşullu eşik kullanılmaz.
            </FormDescription>
            <FormField control={form.control} name="conditionCourseCodes" render={({ field }) => (
              <FormItem>
                <FormLabel>Koşul Ders Kodları <span className="text-muted-foreground text-xs">(virgülle ayırın)</span></FormLabel>
                <FormControl><Input placeholder="örn. BBM384, BBM461" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="minCourseCountIfMet" render={({ field }) => (
                <FormItem>
                  <FormLabel>Alt. Min. Ders <span className="text-muted-foreground text-xs">(koşul karşılanınca)</span></FormLabel>
                  <FormControl><Input type="number" min={0} placeholder="—" {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="minEctsIfMet" render={({ field }) => (
                <FormItem>
                  <FormLabel>Alt. Min. AKTS <span className="text-muted-foreground text-xs">(koşul karşılanınca)</span></FormLabel>
                  <FormControl><Input type="number" min={0} placeholder="—" {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
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

const prefixLimitSchema = z.object({
  courseCodePrefix: z.string().min(1, "Prefix zorunludur").max(10, "En fazla 10 karakter"),
  maxCount: z.coerce.number().int().min(1, "En az 1 olmalıdır"),
});
type PrefixLimitFormValues = z.infer<typeof prefixLimitSchema>;

interface PrefixLimitsDialogProps {
  catId: string;
  catName: string;
  prefixLimits: PrefixLimit[];
  open: boolean;
  onOpenChange: (v: boolean) => void;
}

function PrefixLimitsDialog({ catId, catName, prefixLimits, open, onOpenChange }: PrefixLimitsDialogProps) {
  const qc = useQueryClient();

  const form = useForm<PrefixLimitFormValues>({
    resolver: zodResolver(prefixLimitSchema),
    defaultValues: { courseCodePrefix: "", maxCount: 3 },
  });

  const addMut = useMutation({
    mutationFn: (data: PrefixLimitFormValues) =>
      addPrefixLimit(catId, { courseCodePrefix: data.courseCodePrefix.toUpperCase(), maxCount: data.maxCount }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["categories"] });
      form.reset();
      toast.success("Prefix limiti eklendi.");
    },
    onError: () => toast.error("Prefix limiti eklenemedi."),
  });

  const removeMut = useMutation({
    mutationFn: (limitId: string) => deletePrefixLimit(catId, limitId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["categories"] });
      toast.success("Prefix limiti silindi.");
    },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg shadow-xl">
        <DialogHeader>
          <DialogTitle>{catName} — Prefix Limitleri</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground -mt-2">
          Belirtilen ön eke sahip derslerden en fazla kaç tanesi bu kategoriye sayılabileceğini tanımlayın.
        </p>

        {prefixLimits.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-4 border rounded-md">
            Henüz prefix limiti tanımlanmamış.
          </p>
        ) : (
          <div className="rounded-md border divide-y">
            {prefixLimits.map((limit) => (
              <div key={limit.id} className="flex items-center justify-between px-3 py-2 hover:bg-muted/50">
                <div className="flex items-center gap-3">
                  <Badge variant="outline" className="font-mono text-xs">{limit.courseCodePrefix}</Badge>
                  <span className="text-sm text-muted-foreground">en fazla <span className="font-medium text-foreground">{limit.maxCount}</span> ders</span>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-destructive"
                  onClick={() => removeMut.mutate(limit.id)}
                  disabled={removeMut.isPending}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            ))}
          </div>
        )}

        <Separator />
        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Yeni Limit Ekle</p>
        <Form {...form}>
          <form onSubmit={form.handleSubmit((v) => addMut.mutate(v))} className="flex gap-3 items-end">
            <FormField control={form.control} name="courseCodePrefix" render={({ field }) => (
              <FormItem className="flex-1">
                <FormLabel>Ders Kodu Prefixi</FormLabel>
                <FormControl><Input placeholder="örn. SEC" className="uppercase" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="maxCount" render={({ field }) => (
              <FormItem className="w-28">
                <FormLabel>Maks. Ders</FormLabel>
                <FormControl><Input type="number" min={1} {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <Button type="submit" disabled={addMut.isPending} className="mb-0.5">
              <Plus className="h-4 w-4 mr-1" />Ekle
            </Button>
          </form>
        </Form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Kapat</Button>
        </DialogFooter>
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
  const [prefixLimitCat, setPrefixLimitCat] = useState<Category | undefined>();

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
                  <TableHead>Kohort Aralığı</TableHead>
                  <TableHead className="w-32 text-right">İşlemler</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {categories?.length === 0 && (
                  <TableRow><TableCell colSpan={6} className="text-center text-muted-foreground py-6">Bu bölüme ait kategori yok.</TableCell></TableRow>
                )}
                {categories?.map((cat) => (
                  <TableRow key={cat.id} className="hover:bg-muted/50 transition-colors duration-150">
                    <TableCell className="font-medium">{cat.name}</TableCell>
                    <TableCell className="text-right tabular-nums">{cat.minCourseCount}</TableCell>
                    <TableCell className="text-right tabular-nums text-muted-foreground">{cat.minCredit ?? "—"}</TableCell>
                    <TableCell className="text-right tabular-nums text-muted-foreground">{cat.minEcts ?? "—"}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {cat.appliesFromYear || cat.appliesToYear
                        ? `${cat.appliesFromYear ?? "∞"} – ${cat.appliesToYear ?? "∞"}`
                        : "—"}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" onClick={() => setPoolCat(cat)} aria-label="Ders havuzu" title="Ders havuzu">
                          <BookOpen className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" onClick={() => setPrefixLimitCat(cat)} aria-label="Prefix limitleri" title="Prefix limitleri">
                          <Filter className="h-4 w-4" />
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
      {prefixLimitCat && (
        <PrefixLimitsDialog
          catId={prefixLimitCat.id}
          catName={prefixLimitCat.name}
          prefixLimits={prefixLimitCat.prefixLimits ?? []}
          open={!!prefixLimitCat}
          onOpenChange={(v) => { if (!v) setPrefixLimitCat(undefined); }}
        />
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
