import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, BookOpen, ShieldAlert, Filter } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage, FormDescription } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { matchesTextFilter } from "@/lib/textFilter";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import {
  getDepartments, createDepartment, updateDepartment, deleteDepartment,
  getDepartmentCoursePool, addCourseToDepartment, removeCourseFromDepartment,
} from "@/api/ruleApi";
import type { Department, CreateDepartmentRequest, UpdateDepartmentRequest, DepartmentCourse, DepartmentCoursePoolResponse } from "@/types";

const schema = z.object({
  name: z.string().min(1, "İsim zorunludur"),
  code: z.string().min(1, "Kod zorunludur"),
  description: z.string().optional(),
  minTotalEcts: z.coerce
    .number({ invalid_type_error: "Geçerli bir sayı giriniz" })
    .positive("Pozitif bir değer giriniz")
    .optional()
    .or(z.literal("")),
  blockOnAnyFGrade: z.enum(["true", "false"]),
});
type FormValues = z.infer<typeof schema>;

interface DeptDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initial?: Department;
  onSave: (data: CreateDepartmentRequest | UpdateDepartmentRequest) => Promise<void>;
}

function DeptDialog({ open, onOpenChange, initial, onSave }: DeptDialogProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: initial
      ? {
          name: initial.name,
          code: initial.code,
          description: initial.description ?? "",
          minTotalEcts: initial.minTotalEcts ?? ("" as const),
          blockOnAnyFGrade: initial.blockOnAnyFGrade ? "true" : "false",
        }
      : { name: "", code: "", description: "", minTotalEcts: "", blockOnAnyFGrade: "false" },
  });

  async function onSubmit(values: FormValues) {
    const payload: CreateDepartmentRequest = {
      name: values.name,
      code: values.code,
      description: values.description || undefined,
      minTotalEcts: values.minTotalEcts !== "" && values.minTotalEcts !== undefined
        ? Number(values.minTotalEcts)
        : null,
      blockOnAnyFGrade: values.blockOnAnyFGrade === "true",
    };
    await onSave(payload);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md shadow-xl">
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
            <FormField control={form.control} name="description" render={({ field }) => (
              <FormItem>
                <FormLabel>Açıklama <span className="text-muted-foreground font-normal">(opsiyonel)</span></FormLabel>
                <FormControl><Input placeholder="Bölüm açıklaması..." {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />

            <Separator />
            <p className="text-sm font-medium text-muted-foreground">Küresel Mezuniyet Kuralları</p>

            <FormField control={form.control} name="minTotalEcts" render={({ field }) => (
              <FormItem>
                <FormLabel>Minimum Toplam ECTS <span className="text-muted-foreground font-normal">(opsiyonel)</span></FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={0}
                    step={0.5}
                    placeholder="240"
                    {...field}
                    value={field.value ?? ""}
                  />
                </FormControl>
                <FormDescription>Boş bırakılırsa ECTS eşiği uygulanmaz.</FormDescription>
                <FormMessage />
              </FormItem>
            )} />

            <FormField control={form.control} name="blockOnAnyFGrade" render={({ field }) => (
              <FormItem>
                <FormLabel>F Notu Engeli</FormLabel>
                <Select onValueChange={field.onChange} value={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="false">Hayır — F notu mezuniyeti engellemez</SelectItem>
                    <SelectItem value="true">Evet — Herhangi bir F notu mezuniyeti engeller</SelectItem>
                  </SelectContent>
                </Select>
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

const POOL_KEY = (departmentId: string) => ["department-course-pool", departmentId];

function DeptCoursesPoolDialog({ departmentId, departmentName, open, onOpenChange }: DeptCoursesPoolDialogProps) {
  const qc = useQueryClient();

  const { data: pool, isLoading } = useQuery({
    queryKey: POOL_KEY(departmentId),
    queryFn: () => getDepartmentCoursePool(departmentId),
    enabled: open,
  });

  const assignedCourses: DepartmentCourse[] = pool?.assignedCourses ?? [];
  const availableCourses = pool?.availableCourses ?? [];

  const addMut = useMutation({
    mutationFn: (courseId: string) => addCourseToDepartment(departmentId, courseId),
    onMutate: async (courseId: string) => {
      await qc.cancelQueries({ queryKey: POOL_KEY(departmentId) });
      const previous = qc.getQueryData<DepartmentCoursePoolResponse>(POOL_KEY(departmentId));
      const courseToAdd = (previous?.availableCourses ?? []).find((c) => c.id === courseId);
      if (courseToAdd && previous) {
        qc.setQueryData<DepartmentCoursePoolResponse>(POOL_KEY(departmentId), {
          assignedCourses: [
            ...previous.assignedCourses,
            { courseId: courseToAdd.id, courseCode: courseToAdd.courseCode, courseName: courseToAdd.courseName, credit: courseToAdd.credit, ects: courseToAdd.ects },
          ],
          availableCourses: previous.availableCourses.filter((c) => c.id !== courseId),
        });
      }
      return { previous };
    },
    onError: (_err, _courseId, context) => {
      if (context?.previous !== undefined) {
        qc.setQueryData(POOL_KEY(departmentId), context.previous);
      }
      toast.error("Ders eklenemedi.");
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: POOL_KEY(departmentId) }); toast.success("Ders havuza eklendi."); },
  });

  const removeMut = useMutation({
    mutationFn: (courseId: string) => removeCourseFromDepartment(departmentId, courseId),
    onMutate: async (courseId: string) => {
      await qc.cancelQueries({ queryKey: POOL_KEY(departmentId) });
      const previous = qc.getQueryData<DepartmentCoursePoolResponse>(POOL_KEY(departmentId));
      const courseToRemove = (previous?.assignedCourses ?? []).find((c) => c.courseId === courseId);
      if (courseToRemove && previous) {
        qc.setQueryData<DepartmentCoursePoolResponse>(POOL_KEY(departmentId), {
          assignedCourses: previous.assignedCourses.filter((c) => c.courseId !== courseId),
          availableCourses: [
            ...previous.availableCourses,
            { id: courseToRemove.courseId, courseCode: courseToRemove.courseCode, courseName: courseToRemove.courseName, credit: courseToRemove.credit, ects: courseToRemove.ects },
          ],
        });
      }
      return { previous };
    },
    onError: (_err, _courseId, context) => {
      if (context?.previous !== undefined) {
        qc.setQueryData(POOL_KEY(departmentId), context.previous);
      }
      toast.error("Ders çıkarılamadı.");
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: POOL_KEY(departmentId) }); toast.success("Ders havuzdan çıkarıldı."); },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[min(95vw,1000px)] max-w-none shadow-xl">
        <DialogHeader>
          <DialogTitle>{departmentName} — Ders Havuzu</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground -mt-1">
          Bu bölümün ders havuzuna ders ekleyin veya çıkarın. Havuzdaki dersler mezuniyet kategorilerine atanabilir.
        </p>
        <div className="grid grid-cols-2 gap-6 pt-2">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium">Havuzdaki Dersler</p>
            {isLoading ? (
              <Skeleton className="h-32 w-full" />
            ) : (
              <div className="space-y-1 max-h-72 overflow-y-auto rounded-md border p-2">
                {assignedCourses.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center py-4">Henüz ders yok</p>
                )}
                {assignedCourses.map((c: DepartmentCourse) => (
                  <div key={c.courseId} className="flex items-center gap-2 rounded px-2 py-1.5 hover:bg-muted/50">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium leading-tight truncate">{c.courseName}</p>
                      <p className="font-mono text-xs text-muted-foreground">{c.courseCode}</p>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 shrink-0 text-destructive"
                      onClick={() => removeMut.mutate(c.courseId)}
                      aria-label="Havuzdan çıkar"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium">Eklenebilir Dersler</p>
            <div className="space-y-1 max-h-72 overflow-y-auto rounded-md border p-2">
              {availableCourses.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">Eklenecek ders yok</p>
              )}
              {availableCourses.map((c) => (
                <div key={c.id} className="flex items-center gap-2 rounded px-2 py-1.5 hover:bg-muted/50">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium leading-tight truncate">{c.courseName}</p>
                    <p className="font-mono text-xs text-muted-foreground">{c.courseCode}</p>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-7 shrink-0 text-xs"
                    onClick={() => addMut.mutate(c.id)}
                    aria-label="Havuza ekle"
                  >
                    <Plus className="h-3.5 w-3.5 mr-1" />
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
  const [nameFilter, setNameFilter] = useState("");
  const [codeFilter, setCodeFilter] = useState("");

  const { data: departments, isLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  const filteredDepartments = useMemo(() => {
    return (departments ?? []).filter((d) => {
      if (!matchesTextFilter(d.name, nameFilter)) return false;
      if (!matchesTextFilter(d.code, codeFilter)) return false;
      return true;
    });
  }, [departments, nameFilter, codeFilter]);

  const hasActiveFilters = nameFilter.trim() !== "" || codeFilter.trim() !== "";

  const activeFilterCount = [
    nameFilter.trim() !== "",
    codeFilter.trim() !== "",
  ].filter(Boolean).length;

  function clearFilters() {
    setNameFilter("");
    setCodeFilter("");
  }

  const createMut = useMutation({
    mutationFn: createDepartment,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["departments"] }); toast.success("Bölüm eklendi."); },
    onError: () => toast.error("Bölüm eklenemedi."),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateDepartmentRequest }) => updateDepartment(id, data),
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

  async function handleSave(data: CreateDepartmentRequest | UpdateDepartmentRequest) {
    if (editTarget) {
      await updateMut.mutateAsync({ id: editTarget.id, data: data as UpdateDepartmentRequest });
    } else {
      await createMut.mutateAsync(data as CreateDepartmentRequest);
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Bölümler</h2>
        <div className="flex items-center gap-2">
          <Popover>
            <PopoverTrigger
              render={
                <Button
                  variant="outline"
                  size="sm"
                  className="transition-colors duration-150"
                  aria-pressed={hasActiveFilters}
                  aria-label="Bölümleri filtrele"
                />
              }
            >
              <Filter className="mr-1 h-4 w-4" />
              Filtre
              {hasActiveFilters && (
                <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">
                  {activeFilterCount}
                </Badge>
              )}
            </PopoverTrigger>
            <PopoverContent align="end" className="w-80 shadow-md">
              <PopoverHeader>
                <PopoverTitle>Bölümleri Filtrele</PopoverTitle>
              </PopoverHeader>
              <div className="flex flex-col gap-3">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="department-name-filter" className="text-sm font-medium text-muted-foreground">
                    Bölüm Adı
                  </label>
                  <Input
                    id="department-name-filter"
                    placeholder="Bölüm adına göre filtrele"
                    value={nameFilter}
                    onChange={(e) => setNameFilter(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="department-code-filter" className="text-sm font-medium text-muted-foreground">
                    Kod
                  </label>
                  <Input
                    id="department-code-filter"
                    className="font-mono"
                    placeholder="Koda göre filtrele"
                    value={codeFilter}
                    onChange={(e) => setCodeFilter(e.target.value)}
                  />
                </div>
                {hasActiveFilters && (
                  <Button variant="ghost" onClick={clearFilters} className="self-start transition-colors duration-150">
                    Temizle
                  </Button>
                )}
              </div>
            </PopoverContent>
          </Popover>
          <Button size="sm" onClick={openCreate} className="transition-colors duration-150">
            <Plus className="mr-1 h-4 w-4" />
            Yeni Bölüm
          </Button>
        </div>
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
                <TableHead>Min. ECTS</TableHead>
                <TableHead>F Notu Engeli</TableHead>
                <TableHead className="w-32 text-right">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {departments?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    Henüz bölüm eklenmemiş.
                  </TableCell>
                </TableRow>
              )}
              {departments && departments.length > 0 && filteredDepartments.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    Filtreye uygun bölüm bulunamadı.
                  </TableCell>
                </TableRow>
              )}
              {filteredDepartments.map((d) => (
                <TableRow key={d.id} className="hover:bg-muted/50 transition-colors duration-150">
                  <TableCell className="font-medium">{d.name}</TableCell>
                  <TableCell className="font-mono text-sm text-muted-foreground">{d.code}</TableCell>
                  <TableCell className="text-sm">
                    {d.minTotalEcts != null
                      ? <span className="font-medium">{d.minTotalEcts}</span>
                      : <span className="text-muted-foreground">—</span>}
                  </TableCell>
                  <TableCell>
                    {d.blockOnAnyFGrade
                      ? (
                        <Badge variant="destructive" className="gap-1 text-xs">
                          <ShieldAlert className="h-3 w-3" />
                          Aktif
                        </Badge>
                      )
                      : <span className="text-muted-foreground text-sm">—</span>}
                  </TableCell>
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
