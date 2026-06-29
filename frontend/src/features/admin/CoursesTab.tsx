import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, Filter } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { DepartmentMultiSelect } from "@/components/DepartmentMultiSelect";
import { matchesTextFilter } from "@/lib/textFilter";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import {
  getCourses, createCourse, updateCourse, deleteCourse,
  getDepartments, addCourseToDepartment, removeCourseFromDepartment,
} from "@/api/ruleApi";
import type { Course, CreateCourseRequest } from "@/types";

const schema = z.object({
  courseCode: z.string().min(1, "Ders kodu zorunludur"),
  courseName: z.string().min(1, "Ders adı zorunludur"),
  credit: z.coerce.number().min(0, "Kredi 0 veya üzeri olmalıdır"),
  ects: z.coerce.number().min(0, "AKTS 0 veya üzeri olmalıdır"),
  departmentIds: z.array(z.string()),
});
type FormValues = z.infer<typeof schema>;

interface CourseDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initial?: Course;
  onSave: (data: CreateCourseRequest, departmentIds: string[], initialDepartmentIds: string[]) => Promise<void>;
}

function CourseDialog({ open, onOpenChange, initial, onSave }: CourseDialogProps) {
  const initialDepartmentIds = initial?.departmentIds ?? [];

  const { data: departments, isLoading: departmentsLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
    enabled: open,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: initial
      ? {
          courseCode: initial.courseCode,
          courseName: initial.courseName,
          credit: initial.credit,
          ects: initial.ects,
          departmentIds: initialDepartmentIds,
        }
      : { courseCode: "", courseName: "", credit: 0, ects: 0, departmentIds: [] },
  });

  async function onSubmit(values: FormValues) {
    await onSave(
      {
        courseCode: values.courseCode,
        courseName: values.courseName,
        credit: values.credit,
        ects: values.ects,
      },
      values.departmentIds,
      initialDepartmentIds,
    );
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md shadow-xl">
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
            <FormField control={form.control} name="courseName" render={({ field }) => (
              <FormItem>
                <FormLabel>Ders Adı</FormLabel>
                <FormControl><Input placeholder="Algoritma ve Programlamaya Giriş" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="credit" render={({ field }) => (
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
            <FormField control={form.control} name="departmentIds" render={({ field }) => (
              <FormItem>
                <FormLabel>Bölümler</FormLabel>
                <FormControl>
                  <DepartmentMultiSelect
                    departments={departments ?? []}
                    value={field.value}
                    onChange={field.onChange}
                    isLoading={departmentsLoading}
                  />
                </FormControl>
                <FormDescription>
                  Seçilen bölümlerin ders havuzuna otomatik eklenir. Bölüm sayfasından da yönetilebilir.
                </FormDescription>
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

/**
 * Admin tab: manage the global course catalog.
 */
export function CoursesTab() {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Course | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<Course | undefined>();
  const [courseCodeFilter, setCourseCodeFilter] = useState("");
  const [courseNameFilter, setCourseNameFilter] = useState("");

  const { data: courses, isLoading } = useQuery({
    queryKey: ["courses"],
    queryFn: getCourses,
  });

  const { data: departments } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  const departmentNameById = useMemo(
    () => new Map((departments ?? []).map((d) => [d.id, d.name])),
    [departments],
  );

  const filteredCourses = useMemo(() => {
    return (courses ?? []).filter((c) => {
      if (!matchesTextFilter(c.courseCode, courseCodeFilter)) return false;
      if (!matchesTextFilter(c.courseName, courseNameFilter)) return false;
      return true;
    });
  }, [courses, courseCodeFilter, courseNameFilter]);

  const hasActiveFilters = courseCodeFilter.trim() !== "" || courseNameFilter.trim() !== "";

  const activeFilterCount = [
    courseCodeFilter.trim() !== "",
    courseNameFilter.trim() !== "",
  ].filter(Boolean).length;

  function clearFilters() {
    setCourseCodeFilter("");
    setCourseNameFilter("");
  }

  function invalidateCourseQueries() {
    qc.invalidateQueries({ queryKey: ["courses"] });
    qc.invalidateQueries({ queryKey: ["department-course-pool"] });
  }

  const createMut = useMutation({
    mutationFn: createCourse,
    onSuccess: () => { invalidateCourseQueries(); toast.success("Ders eklendi."); },
    onError: () => toast.error("Ders eklenemedi."),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateCourseRequest }) => updateCourse(id, data),
    onSuccess: () => { invalidateCourseQueries(); toast.success("Ders güncellendi."); },
    onError: () => toast.error("Güncelleme başarısız."),
  });

  const deleteMut = useMutation({
    mutationFn: deleteCourse,
    onSuccess: () => { invalidateCourseQueries(); toast.success("Ders silindi."); setDeleteTarget(undefined); },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  async function handleSave(
    data: CreateCourseRequest,
    departmentIds: string[],
    initialDepartmentIds: string[],
  ) {
    const course = editTarget
      ? await updateMut.mutateAsync({ id: editTarget.id, data })
      : await createMut.mutateAsync(data);

    const toAdd = departmentIds.filter((id) => !initialDepartmentIds.includes(id));
    const toRemove = initialDepartmentIds.filter((id) => !departmentIds.includes(id));

    if (toAdd.length === 0 && toRemove.length === 0) {
      return;
    }

    try {
      await Promise.all([
        ...toAdd.map((id) => addCourseToDepartment(id, course.id)),
        ...toRemove.map((id) => removeCourseFromDepartment(id, course.id)),
      ]);
      invalidateCourseQueries();
    } catch {
      toast.error("Ders kaydedildi ancak bölüm bağlantısı güncellenemedi.");
      throw new Error("Department link update failed");
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Ders Kataloğu</h2>
        <div className="flex items-center gap-2">
          <Popover>
            <PopoverTrigger
              render={
                <Button
                  variant="outline"
                  size="sm"
                  className="transition-colors duration-150"
                  aria-pressed={hasActiveFilters}
                  aria-label="Dersleri filtrele"
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
                <PopoverTitle>Dersleri Filtrele</PopoverTitle>
              </PopoverHeader>
              <div className="flex flex-col gap-3">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="course-code-filter" className="text-sm font-medium text-muted-foreground">
                    Ders Kodu
                  </label>
                  <Input
                    id="course-code-filter"
                    className="font-mono"
                    placeholder="Ders koduna göre filtrele"
                    value={courseCodeFilter}
                    onChange={(e) => setCourseCodeFilter(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="course-name-filter" className="text-sm font-medium text-muted-foreground">
                    Ders Adı
                  </label>
                  <Input
                    id="course-name-filter"
                    placeholder="Ders adına göre filtrele"
                    value={courseNameFilter}
                    onChange={(e) => setCourseNameFilter(e.target.value)}
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
          <Button size="sm" onClick={() => { setEditTarget(undefined); setDialogOpen(true); }} className="transition-colors duration-150">
            <Plus className="mr-1 h-4 w-4" />
            Yeni Ders
          </Button>
        </div>
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
                <TableHead>Bölümler</TableHead>
                <TableHead className="text-right">Kredi</TableHead>
                <TableHead className="text-right">AKTS</TableHead>
                <TableHead className="w-24 text-right">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {courses?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    Ders kataloğu boş.
                  </TableCell>
                </TableRow>
              )}
              {courses && courses.length > 0 && filteredCourses.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    Filtreye uygun ders bulunamadı.
                  </TableCell>
                </TableRow>
              )}
              {filteredCourses.map((c) => (
                <TableRow key={c.id} className="hover:bg-muted/50 transition-colors duration-150">
                  <TableCell className="font-mono text-sm">{c.courseCode}</TableCell>
                  <TableCell className="font-medium">{c.courseName}</TableCell>
                  <TableCell>
                    {(c.departmentIds?.length ?? 0) === 0 ? (
                      <span className="text-sm text-muted-foreground">—</span>
                    ) : (
                      <div className="flex flex-wrap gap-1">
                        {c.departmentIds?.map((id) => (
                          <Badge key={id} variant="outline" className="text-xs font-normal">
                            {departmentNameById.get(id) ?? id}
                          </Badge>
                        ))}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{c.credit}</TableCell>
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
        description={`"${deleteTarget?.courseCode} — ${deleteTarget?.courseName}" dersini katalogdan silmek istediğinize emin misiniz?`}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />
    </section>
  );
}
