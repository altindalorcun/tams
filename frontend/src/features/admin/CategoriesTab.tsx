import { useEffect, useMemo, useState } from "react";
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
import { DepartmentCoursePicker } from "@/components/DepartmentCoursePicker";
import { YearPickerField } from "@/components/YearPickerField";
import { CohortBoundaryFields, formatCohortBoundary, type CohortBoundaryValue } from "@/components/CohortBoundaryFields";
import { matchesTextFilter } from "@/lib/textFilter";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import {
  getDepartments, getCategories, createCategory, updateCategory, deleteCategory,
  getCategoryCoursePool, addCourseToCategory, updateCategoryCourse, removeCourseFromCategory,
} from "@/api/ruleApi";
import type {
  Category, CreateCategoryRequest, CategoryCourse, CategoryCoursePoolResponse, DepartmentCourse,
  CreatePrefixLimitRequest, CategoryCourseRequest, UpdateCategoryCourseRequest, EnrollmentTerm,
} from "@/types";

const prefixLimitEntrySchema = z.object({
  courseCodePrefix: z.string().min(1, "Prefix zorunludur").max(10, "En fazla 10 karakter"),
  maxCount: z.coerce.number().int().min(1, "En az 1 olmalıdır"),
});

const catSchema = z.object({
  name: z.string().min(1, "Kategori adı zorunludur"),
  minCourseCount: z.coerce.number().int().min(0),
  minCredit: z.coerce.number().min(0),
  minEcts: z.coerce.number().min(0),
  appliesFromYear: z.coerce.number().int().positive().optional().or(z.literal("")),
  appliesToYear: z.coerce.number().int().positive().optional().or(z.literal("")),
  conditionCourseCodes: z.array(z.string()),
  minCourseCountIfMet: z.coerce.number().int().min(0).optional().or(z.literal("")),
  minEctsIfMet: z.coerce.number().min(0).optional().or(z.literal("")),
  prefixLimits: z.array(prefixLimitEntrySchema),
});
type CatFormValues = z.infer<typeof catSchema>;

const EMPTY_CAT_VALUES: CatFormValues = {
  name: "",
  minCourseCount: 0,
  minCredit: 0,
  minEcts: 0,
  appliesFromYear: "",
  appliesToYear: "",
  conditionCourseCodes: [],
  minCourseCountIfMet: "",
  minEctsIfMet: "",
  prefixLimits: [],
};

function toCatFormValues(initial?: Category): CatFormValues {
  if (!initial) return EMPTY_CAT_VALUES;
  return {
    name: initial.name,
    minCourseCount: initial.minCourseCount,
    minCredit: initial.minCredit ?? 0,
    minEcts: initial.minEcts ?? 0,
    appliesFromYear: initial.appliesFromYear ?? ("" as const),
    appliesToYear: initial.appliesToYear ?? ("" as const),
    conditionCourseCodes: initial.conditionCourseCodes ?? [],
    minCourseCountIfMet: initial.minCourseCountIfMet ?? ("" as const),
    minEctsIfMet: initial.minEctsIfMet ?? ("" as const),
    prefixLimits: (initial.prefixLimits ?? []).map(({ courseCodePrefix, maxCount }) => ({
      courseCodePrefix,
      maxCount,
    })),
  };
}

function toCreateCategoryRequest(v: CatFormValues): CreateCategoryRequest {
  return {
    name: v.name,
    minCourseCount: v.minCourseCount,
    minCredit: v.minCredit,
    minEcts: v.minEcts,
    appliesFromYear: v.appliesFromYear === "" || v.appliesFromYear === undefined ? null : Number(v.appliesFromYear),
    appliesToYear: v.appliesToYear === "" || v.appliesToYear === undefined ? null : Number(v.appliesToYear),
    conditionCourseCodes: v.conditionCourseCodes.length > 0 ? v.conditionCourseCodes : null,
    minCourseCountIfMet: v.minCourseCountIfMet === "" || v.minCourseCountIfMet === undefined ? null : Number(v.minCourseCountIfMet),
    minEctsIfMet: v.minEctsIfMet === "" || v.minEctsIfMet === undefined ? null : Number(v.minEctsIfMet),
    prefixLimits: v.prefixLimits.length > 0
      ? v.prefixLimits.map((limit) => ({
          courseCodePrefix: limit.courseCodePrefix.toUpperCase(),
          maxCount: limit.maxCount,
        }))
      : [],
  };
}

interface PrefixLimitsSectionProps {
  limits: CreatePrefixLimitRequest[];
  onChange: (limits: CreatePrefixLimitRequest[]) => void;
}

/** Inline editor for course-code prefix sub-limits within the category form. */
function PrefixLimitsSection({ limits, onChange }: PrefixLimitsSectionProps) {
  const [draftPrefix, setDraftPrefix] = useState("");
  const [draftMaxCount, setDraftMaxCount] = useState("3");
  const [draftError, setDraftError] = useState<string | null>(null);

  function handleAdd() {
    const parsed = prefixLimitEntrySchema.safeParse({
      courseCodePrefix: draftPrefix,
      maxCount: draftMaxCount,
    });
    if (!parsed.success) {
      setDraftError(parsed.error.issues[0]?.message ?? "Geçersiz değer");
      return;
    }
    setDraftError(null);
    onChange([
      ...limits,
      {
        courseCodePrefix: parsed.data.courseCodePrefix.toUpperCase(),
        maxCount: parsed.data.maxCount,
      },
    ]);
    setDraftPrefix("");
    setDraftMaxCount("3");
  }

  function handleRemove(index: number) {
    onChange(limits.filter((_, i) => i !== index));
  }

  return (
    <div className="space-y-3">
      {limits.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-3 border rounded-md">
          Henüz prefix limiti tanımlanmamış.
        </p>
      ) : (
        <div className="rounded-md border divide-y">
          {limits.map((limit, index) => (
            <div key={`${limit.courseCodePrefix}-${index}`} className="flex items-center justify-between px-3 py-2 hover:bg-muted/50">
              <div className="flex items-center gap-3">
                <Badge variant="outline" className="font-mono text-xs">{limit.courseCodePrefix}</Badge>
                <span className="text-sm text-muted-foreground">
                  en fazla <span className="font-medium text-foreground">{limit.maxCount}</span> ders
                </span>
              </div>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-7 w-7 text-destructive"
                aria-label="Prefix limitini kaldır"
                onClick={() => handleRemove(index)}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>
          ))}
        </div>
      )}
      <div className="flex gap-3 items-end">
        <div className="flex-1 space-y-1.5">
          <label htmlFor="prefix-limit-prefix" className="text-sm font-medium">Ders Kodu Prefixi</label>
          <Input
            id="prefix-limit-prefix"
            placeholder="örn. SEC"
            className="uppercase"
            value={draftPrefix}
            onChange={(e) => setDraftPrefix(e.target.value)}
          />
        </div>
        <div className="w-28 space-y-1.5">
          <label htmlFor="prefix-limit-max" className="text-sm font-medium">Maks. Ders</label>
          <Input
            id="prefix-limit-max"
            type="number"
            min={1}
            value={draftMaxCount}
            onChange={(e) => setDraftMaxCount(e.target.value)}
          />
        </div>
        <Button type="button" className="mb-0.5" onClick={handleAdd}>
          <Plus className="h-4 w-4 mr-1" />Ekle
        </Button>
      </div>
      {draftError && <p className="text-sm text-destructive">{draftError}</p>}
    </div>
  );
}

interface CatDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  departmentId: string;
  initial?: Category;
  onSave: (data: CreateCategoryRequest) => Promise<void>;
}

function CatDialog({ open, onOpenChange, departmentId, initial, onSave }: CatDialogProps) {
  const form = useForm<CatFormValues>({
    resolver: zodResolver(catSchema),
    values: toCatFormValues(initial),
  });

  async function onSubmit(v: CatFormValues) {
    await onSave(toCreateCategoryRequest(v));
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
              <FormItem>
                <FormLabel>Minimum Ders Sayısı</FormLabel>
                <FormControl><Input type="number" min={0} {...field} /></FormControl>
                <FormDescription className="text-xs">0 girerseniz bu eşik dikkate alınmaz.</FormDescription>
                <FormMessage />
              </FormItem>
            )} />
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="minCredit" render={({ field }) => (
                <FormItem>
                  <FormLabel>Min. Kredi</FormLabel>
                  <FormControl><Input type="number" min={0} placeholder="0" {...field} /></FormControl>
                  <FormDescription className="text-xs">0 = kontrol edilmez</FormDescription>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="minEcts" render={({ field }) => (
                <FormItem>
                  <FormLabel>Min. AKTS</FormLabel>
                  <FormControl><Input type="number" min={0} placeholder="0" {...field} /></FormControl>
                  <FormDescription className="text-xs">0 = kontrol edilmez</FormDescription>
                  <FormMessage />
                </FormItem>
              )} />
            </div>
            <Separator />
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Kohort Aralığı</p>
            <FormDescription className="text-xs -mt-2">Bu kategori yalnızca belirtilen kayıt yılı aralığındaki öğrencilere uygulanır. Boş bırakırsanız sınır yoktur.</FormDescription>
            <div className="grid grid-cols-2 gap-3">
              <FormField control={form.control} name="appliesFromYear" render={({ field }) => (
                <FormItem>
                  <FormLabel>Başlangıç Yılı</FormLabel>
                  <FormControl>
                    <YearPickerField
                      id="applies-from-year"
                      value={field.value}
                      onChange={field.onChange}
                      placeholder="Başlangıç yılı seçin"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="appliesToYear" render={({ field }) => (
                <FormItem>
                  <FormLabel>Bitiş Yılı</FormLabel>
                  <FormControl>
                    <YearPickerField
                      id="applies-to-year"
                      value={field.value}
                      onChange={field.onChange}
                      placeholder="Bitiş yılı seçin"
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
            </div>
            <Separator />
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Koşullu Eşikler</p>
            <FormDescription className="text-xs -mt-2">
              Öğrenci seçilen ders kodlarından herhangi birini geçmişse alternatif eşikler uygulanır.
              Boş bırakılırsa koşullu eşik kullanılmaz.
            </FormDescription>
            <FormField control={form.control} name="conditionCourseCodes" render={({ field }) => (
              <FormItem>
                <FormLabel>Koşul Ders Kodları</FormLabel>
                <FormControl>
                  <DepartmentCoursePicker
                    mode="multiple"
                    departmentId={departmentId}
                    enabled={open}
                    triggerLabel="Koşul dersleri seç"
                    value={field.value}
                    onChange={field.onChange}
                  />
                </FormControl>
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
            <Separator />
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Prefix Limitleri</p>
            <FormDescription className="text-xs -mt-2">
              Belirtilen ön eke sahip derslerden en fazla kaç tanesi bu kategoriye sayılabileceğini tanımlayın.
            </FormDescription>
            <FormField control={form.control} name="prefixLimits" render={({ field }) => (
              <FormItem>
                <FormControl>
                  <PrefixLimitsSection limits={field.value} onChange={field.onChange} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )} />
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

const EMPTY_BOUNDARY: CohortBoundaryValue = { year: "", term: "" };

function boundaryFromCourse(
  year?: number | null,
  term?: EnrollmentTerm | null,
): CohortBoundaryValue {
  return {
    year: year ?? "",
    term: term ?? "",
  };
}

function appliesPayload(from: CohortBoundaryValue, to: CohortBoundaryValue) {
  return {
    appliesFromYear: from.year === "" ? null : Number(from.year),
    appliesFromTerm: from.year === "" ? null : (from.term === "" ? null : from.term),
    appliesToYear: to.year === "" ? null : Number(to.year),
    appliesToTerm: to.year === "" ? null : (to.term === "" ? null : to.term),
  };
}

interface CategoryCourseAssignmentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: "add" | "edit";
  courseCode: string;
  courseName: string;
  isMandatory: boolean;
  appliesFrom: CohortBoundaryValue;
  appliesTo: CohortBoundaryValue;
  onAppliesFromChange: (value: CohortBoundaryValue) => void;
  onAppliesToChange: (value: CohortBoundaryValue) => void;
  onMandatoryChange: (value: boolean) => void;
  onSave: () => Promise<void>;
  isSaving: boolean;
}

/** Dialog for setting mandatory flag and enrollment cohort bounds when assigning a course. */
function CategoryCourseAssignmentDialog({
  open,
  onOpenChange,
  mode,
  courseCode,
  courseName,
  isMandatory,
  appliesFrom,
  appliesTo,
  onAppliesFromChange,
  onAppliesToChange,
  onMandatoryChange,
  onSave,
  isSaving,
}: CategoryCourseAssignmentDialogProps) {
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await onSave();
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md shadow-xl">
        <DialogHeader>
          <DialogTitle>
            {mode === "add" ? "Dersi Kategoriye Ekle" : "Ders Atamasını Düzenle"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-2">
          <div>
            <p className="text-sm font-medium font-mono">{courseCode}</p>
            <p className="text-sm text-muted-foreground">{courseName}</p>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              variant={isMandatory ? "default" : "outline"}
              size="sm"
              onClick={() => onMandatoryChange(true)}
            >
              Zorunlu
            </Button>
            <Button
              type="button"
              variant={!isMandatory ? "default" : "outline"}
              size="sm"
              onClick={() => onMandatoryChange(false)}
            >
              Seçmeli
            </Button>
          </div>
          <Separator />
          <CohortBoundaryFields
            label="Başlangıç Kohortu"
            description="Bu kohort ve sonrası kayıtlılar için ders kategoride geçerlidir. Boş bırakılırsa alt sınır yoktur."
            yearId="applies-from-year"
            termId="applies-from-term"
            value={appliesFrom}
            onChange={onAppliesFromChange}
          />
          <CohortBoundaryFields
            label="Bitiş Kohortu"
            description="Bu kohort ve sonrası kayıtlılar için ders kategoride geçersiz sayılır. Boş bırakılırsa üst sınır yoktur."
            yearId="applies-to-year"
            termId="applies-to-term"
            value={appliesTo}
            onChange={onAppliesToChange}
          />
          <DialogFooter className="pt-2">
            <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>
              İptal
            </Button>
            <Button type="submit" disabled={isSaving}>
              {mode === "add" ? "Ekle" : "Kaydet"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const CATEGORY_POOL_KEY = (catId: string) => ["category-course-pool", catId];

function compareCourseCode(a: string, b: string): number {
  return a.localeCompare(b, undefined, { sensitivity: "base" });
}

function sortAssignedCourses(courses: CategoryCourse[]): CategoryCourse[] {
  return [...courses].sort((a, b) => compareCourseCode(a.courseCode, b.courseCode));
}

function sortAvailableCourses(courses: DepartmentCourse[]): DepartmentCourse[] {
  return [...courses].sort((a, b) => compareCourseCode(a.courseCode, b.courseCode));
}

interface CoursesPoolDialogProps {
  catId: string;
  catName: string;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}

function CoursesPoolDialog({ catId, catName, open, onOpenChange }: CoursesPoolDialogProps) {
  const qc = useQueryClient();
  const [assignedCodeFilter, setAssignedCodeFilter] = useState("");
  const [assignedNameFilter, setAssignedNameFilter] = useState("");
  const [availableCodeFilter, setAvailableCodeFilter] = useState("");
  const [availableNameFilter, setAvailableNameFilter] = useState("");
  const [assignmentOpen, setAssignmentOpen] = useState(false);
  const [assignmentMode, setAssignmentMode] = useState<"add" | "edit">("add");
  const [pendingCourseId, setPendingCourseId] = useState<string | null>(null);
  const [pendingCourseCode, setPendingCourseCode] = useState("");
  const [pendingCourseName, setPendingCourseName] = useState("");
  const [isMandatory, setIsMandatory] = useState(false);
  const [appliesFrom, setAppliesFrom] = useState<CohortBoundaryValue>(EMPTY_BOUNDARY);
  const [appliesTo, setAppliesTo] = useState<CohortBoundaryValue>(EMPTY_BOUNDARY);

  const { data: pool, isLoading } = useQuery({
    queryKey: CATEGORY_POOL_KEY(catId),
    queryFn: () => getCategoryCoursePool(catId),
    enabled: open,
  });

  const catCourses = pool?.assignedCourses ?? [];
  const availableCourses = pool?.availableCourses ?? [];
  const deptPoolSize = catCourses.length + availableCourses.length;

  const filteredCatCourses = useMemo(() => {
    return catCourses.filter((c: CategoryCourse) => {
      if (!matchesTextFilter(c.courseCode, assignedCodeFilter)) return false;
      if (!matchesTextFilter(c.courseName, assignedNameFilter)) return false;
      return true;
    });
  }, [catCourses, assignedCodeFilter, assignedNameFilter]);

  const filteredAvailableCourses = useMemo(() => {
    return availableCourses.filter((c) => {
      if (!matchesTextFilter(c.courseCode, availableCodeFilter)) return false;
      if (!matchesTextFilter(c.courseName, availableNameFilter)) return false;
      return true;
    });
  }, [availableCourses, availableCodeFilter, availableNameFilter]);

  const hasAssignedFilters = assignedCodeFilter.trim() !== "" || assignedNameFilter.trim() !== "";
  const hasAvailableFilters = availableCodeFilter.trim() !== "" || availableNameFilter.trim() !== "";

  const assignedActiveFilterCount = [
    assignedCodeFilter.trim() !== "",
    assignedNameFilter.trim() !== "",
  ].filter(Boolean).length;

  const availableActiveFilterCount = [
    availableCodeFilter.trim() !== "",
    availableNameFilter.trim() !== "",
  ].filter(Boolean).length;

  function clearAllFilters() {
    setAssignedCodeFilter("");
    setAssignedNameFilter("");
    setAvailableCodeFilter("");
    setAvailableNameFilter("");
  }

  function clearAssignedFilters() {
    setAssignedCodeFilter("");
    setAssignedNameFilter("");
  }

  function clearAvailableFilters() {
    setAvailableCodeFilter("");
    setAvailableNameFilter("");
  }

  function handleOpenChange(v: boolean) {
    if (!v) clearAllFilters();
    onOpenChange(v);
  }

  useEffect(() => {
    if (!open) clearAllFilters();
  }, [open]);

  function resetAssignmentForm() {
    setPendingCourseId(null);
    setPendingCourseCode("");
    setPendingCourseName("");
    setIsMandatory(false);
    setAppliesFrom(EMPTY_BOUNDARY);
    setAppliesTo(EMPTY_BOUNDARY);
  }

  function openAddAssignment(courseId: string, courseCode: string, courseName: string, mandatory: boolean) {
    resetAssignmentForm();
    setAssignmentMode("add");
    setPendingCourseId(courseId);
    setPendingCourseCode(courseCode);
    setPendingCourseName(courseName);
    setIsMandatory(mandatory);
    setAssignmentOpen(true);
  }

  function openEditAssignment(course: CategoryCourse) {
    resetAssignmentForm();
    setAssignmentMode("edit");
    setPendingCourseId(course.courseId);
    setPendingCourseCode(course.courseCode);
    setPendingCourseName(course.courseName);
    setIsMandatory(course.isMandatory);
    setAppliesFrom(boundaryFromCourse(course.appliesFromYear, course.appliesFromTerm));
    setAppliesTo(boundaryFromCourse(course.appliesToYear, course.appliesToTerm));
    setAssignmentOpen(true);
  }

  const saveMut = useMutation({
    mutationFn: async (): Promise<{ mode: "add" | "edit"; course: CategoryCourse }> => {
      const bounds = appliesPayload(appliesFrom, appliesTo);
      if (assignmentMode === "add") {
        if (!pendingCourseId) {
          throw new Error("Course id missing");
        }
        const request: CategoryCourseRequest = {
          courseId: pendingCourseId,
          isMandatory,
          ...bounds,
        };
        const course = await addCourseToCategory(catId, request);
        return { mode: "add", course };
      }
      if (!pendingCourseId) {
        throw new Error("Course id missing");
      }
      const request: UpdateCategoryCourseRequest = {
        isMandatory,
        ...bounds,
      };
      const course = await updateCategoryCourse(catId, pendingCourseId, request);
      return { mode: "edit", course };
    },
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: CATEGORY_POOL_KEY(catId) });
      const previous = qc.getQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId));
      if (!previous || !pendingCourseId) {
        return { previous };
      }

      if (assignmentMode === "add") {
        const courseToAdd = previous.availableCourses.find((c) => c.courseId === pendingCourseId);
        if (!courseToAdd) {
          return { previous };
        }
        const bounds = appliesPayload(appliesFrom, appliesTo);
        const optimisticAssigned: CategoryCourse = {
          courseId: courseToAdd.courseId,
          courseCode: courseToAdd.courseCode,
          courseName: courseToAdd.courseName,
          credit: courseToAdd.credit,
          ects: courseToAdd.ects,
          isMandatory,
          ...bounds,
        };
        qc.setQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId), {
          assignedCourses: sortAssignedCourses([...previous.assignedCourses, optimisticAssigned]),
          availableCourses: sortAvailableCourses(
            previous.availableCourses.filter((c) => c.courseId !== pendingCourseId),
          ),
        });
        return { previous };
      }

      const bounds = appliesPayload(appliesFrom, appliesTo);
      qc.setQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId), {
        assignedCourses: sortAssignedCourses(
          previous.assignedCourses.map((c) =>
            c.courseId === pendingCourseId
              ? { ...c, isMandatory, ...bounds }
              : c,
          ),
        ),
        availableCourses: previous.availableCourses,
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      if (context?.previous !== undefined) {
        qc.setQueryData(CATEGORY_POOL_KEY(catId), context.previous);
      }
      toast.error(assignmentMode === "add" ? "Ders eklenemedi." : "Ders ataması güncellenemedi.");
    },
    onSuccess: ({ mode, course }) => {
      const current = qc.getQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId));
      if (current) {
        if (mode === "add") {
          qc.setQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId), {
            assignedCourses: sortAssignedCourses(
              current.assignedCourses.map((c) => (c.courseId === course.courseId ? course : c)),
            ),
            availableCourses: current.availableCourses,
          });
        } else {
          qc.setQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId), {
            assignedCourses: sortAssignedCourses(
              current.assignedCourses.map((c) => (c.courseId === course.courseId ? course : c)),
            ),
            availableCourses: current.availableCourses,
          });
        }
      }
      qc.invalidateQueries({ queryKey: CATEGORY_POOL_KEY(catId) });
      toast.success(mode === "add" ? "Ders eklendi." : "Ders ataması güncellendi.");
      setAssignmentOpen(false);
      resetAssignmentForm();
    },
  });

  const removeMut = useMutation({
    mutationFn: (courseId: string) => removeCourseFromCategory(catId, courseId),
    onMutate: async (courseId: string) => {
      await qc.cancelQueries({ queryKey: CATEGORY_POOL_KEY(catId) });
      const previous = qc.getQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId));
      const courseToRemove = previous?.assignedCourses.find((c) => c.courseId === courseId);
      if (courseToRemove && previous) {
        qc.setQueryData<CategoryCoursePoolResponse>(CATEGORY_POOL_KEY(catId), {
          assignedCourses: previous.assignedCourses.filter((c) => c.courseId !== courseId),
          availableCourses: sortAvailableCourses([
            ...previous.availableCourses,
            {
              courseId: courseToRemove.courseId,
              courseCode: courseToRemove.courseCode,
              courseName: courseToRemove.courseName,
              credit: courseToRemove.credit,
              ects: courseToRemove.ects,
            },
          ]),
        });
      }
      return { previous };
    },
    onError: (_err, _courseId, context) => {
      if (context?.previous !== undefined) {
        qc.setQueryData(CATEGORY_POOL_KEY(catId), context.previous);
      }
      toast.error("Ders çıkarılamadı.");
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_POOL_KEY(catId) });
      toast.success("Ders çıkarıldı.");
    },
  });

  return (
    <>
      <CategoryCourseAssignmentDialog
        open={assignmentOpen}
        onOpenChange={(v) => {
          setAssignmentOpen(v);
          if (!v) resetAssignmentForm();
        }}
        mode={assignmentMode}
        courseCode={pendingCourseCode}
        courseName={pendingCourseName}
        isMandatory={isMandatory}
        appliesFrom={appliesFrom}
        appliesTo={appliesTo}
        onAppliesFromChange={setAppliesFrom}
        onAppliesToChange={setAppliesTo}
        onMandatoryChange={setIsMandatory}
        onSave={async () => {
          await saveMut.mutateAsync();
        }}
        isSaving={saveMut.isPending}
      />
      <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl shadow-xl">
        <DialogHeader><DialogTitle>{catName} — Ders Havuzu</DialogTitle></DialogHeader>
        <div className="grid grid-cols-2 gap-4 pt-2 min-h-[240px]">
          <div>
            <div className="flex items-center justify-between gap-2 mb-2">
              <p className="text-sm font-medium">Kategorideki Dersler</p>
              <Popover>
                <PopoverTrigger
                  render={
                    <Button
                      variant="outline"
                      size="sm"
                      className="h-7 shrink-0 transition-colors duration-150"
                      aria-pressed={hasAssignedFilters}
                      aria-label="Kategorideki dersleri filtrele"
                    />
                  }
                >
                  <Filter className="mr-1 h-3.5 w-3.5" />
                  Filtre
                  {hasAssignedFilters && (
                    <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">
                      {assignedActiveFilterCount}
                    </Badge>
                  )}
                </PopoverTrigger>
                <PopoverContent align="end" className="w-72 shadow-md">
                  <PopoverHeader>
                    <PopoverTitle>Kategorideki Dersleri Filtrele</PopoverTitle>
                  </PopoverHeader>
                  <div className="flex flex-col gap-3">
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="category-pool-assigned-code-filter" className="text-sm font-medium text-muted-foreground">
                        Ders Kodu
                      </label>
                      <Input
                        id="category-pool-assigned-code-filter"
                        className="font-mono"
                        placeholder="Ders koduna göre filtrele"
                        value={assignedCodeFilter}
                        onChange={(e) => setAssignedCodeFilter(e.target.value)}
                      />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="category-pool-assigned-name-filter" className="text-sm font-medium text-muted-foreground">
                        Ders Adı
                      </label>
                      <Input
                        id="category-pool-assigned-name-filter"
                        placeholder="Ders adına göre filtrele"
                        value={assignedNameFilter}
                        onChange={(e) => setAssignedNameFilter(e.target.value)}
                      />
                    </div>
                    {hasAssignedFilters && (
                      <Button variant="ghost" onClick={clearAssignedFilters} className="self-start transition-colors duration-150">
                        Temizle
                      </Button>
                    )}
                  </div>
                </PopoverContent>
              </Popover>
            </div>
            {isLoading ? <Skeleton className="h-32 w-full" /> : (
              <div className="space-y-1 max-h-64 overflow-y-auto rounded-md border p-2">
                {catCourses.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center py-4">Ders yok</p>
                )}
                {catCourses.length > 0 && filteredCatCourses.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center py-4">Filtreye uygun ders bulunamadı.</p>
                )}
                {filteredCatCourses.map((c: CategoryCourse) => {
                  const fromLabel = formatCohortBoundary(c.appliesFromYear, c.appliesFromTerm);
                  const toLabel = formatCohortBoundary(c.appliesToYear, c.appliesToTerm);
                  return (
                  <div key={c.courseId} className="flex items-center justify-between rounded px-2 py-1 hover:bg-muted/50 gap-2">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-1">
                        <span className="font-mono text-xs text-muted-foreground">{c.courseCode}</span>
                        <span className="text-sm truncate">{c.courseName}</span>
                        <Badge variant="outline" className="text-xs">
                          {c.isMandatory ? "Zorunlu" : "Seçmeli"}
                        </Badge>
                      </div>
                      {(fromLabel || toLabel) && (
                        <p className="text-xs text-muted-foreground mt-0.5">
                          {fromLabel && <>Başlangıç: {fromLabel}</>}
                          {fromLabel && toLabel && " · "}
                          {toLabel && <>Bitiş: {toLabel}</>}
                        </p>
                      )}
                    </div>
                    <div className="flex shrink-0 gap-0.5">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-6 w-6"
                        aria-label="Ders atamasını düzenle"
                        onClick={() => openEditAssignment(c)}
                      >
                        <Pencil className="h-3 w-3" />
                      </Button>
                      <Button variant="ghost" size="icon" className="h-6 w-6 text-destructive" onClick={() => removeMut.mutate(c.courseId)}>
                        <Trash2 className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                  );
                })}
              </div>
            )}
          </div>
          <div>
            <div className="flex items-center justify-between gap-2 mb-2">
              <p className="text-sm font-medium">Eklenebilir Dersler</p>
              <Popover>
                <PopoverTrigger
                  render={
                    <Button
                      variant="outline"
                      size="sm"
                      className="h-7 shrink-0 transition-colors duration-150"
                      aria-pressed={hasAvailableFilters}
                      aria-label="Eklenebilir dersleri filtrele"
                    />
                  }
                >
                  <Filter className="mr-1 h-3.5 w-3.5" />
                  Filtre
                  {hasAvailableFilters && (
                    <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">
                      {availableActiveFilterCount}
                    </Badge>
                  )}
                </PopoverTrigger>
                <PopoverContent align="end" className="w-72 shadow-md">
                  <PopoverHeader>
                    <PopoverTitle>Eklenebilir Dersleri Filtrele</PopoverTitle>
                  </PopoverHeader>
                  <div className="flex flex-col gap-3">
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="category-pool-available-code-filter" className="text-sm font-medium text-muted-foreground">
                        Ders Kodu
                      </label>
                      <Input
                        id="category-pool-available-code-filter"
                        className="font-mono"
                        placeholder="Ders koduna göre filtrele"
                        value={availableCodeFilter}
                        onChange={(e) => setAvailableCodeFilter(e.target.value)}
                      />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="category-pool-available-name-filter" className="text-sm font-medium text-muted-foreground">
                        Ders Adı
                      </label>
                      <Input
                        id="category-pool-available-name-filter"
                        placeholder="Ders adına göre filtrele"
                        value={availableNameFilter}
                        onChange={(e) => setAvailableNameFilter(e.target.value)}
                      />
                    </div>
                    {hasAvailableFilters && (
                      <Button variant="ghost" onClick={clearAvailableFilters} className="self-start transition-colors duration-150">
                        Temizle
                      </Button>
                    )}
                  </div>
                </PopoverContent>
              </Popover>
            </div>
            {isLoading ? <Skeleton className="h-32 w-full" /> : (
            <div className="space-y-1 max-h-64 overflow-y-auto rounded-md border p-2">
              {deptPoolSize === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4 px-2">
                  Bu bölümün ders havuzunda ders yok. Önce Ders Kataloğu veya Bölümler sekmesinden ders ekleyin.
                </p>
              )}
              {deptPoolSize > 0 && availableCourses.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">Eklenecek ders kalmadı</p>
              )}
              {availableCourses.length > 0 && filteredAvailableCourses.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">Filtreye uygun ders bulunamadı.</p>
              )}
              {filteredAvailableCourses.map((c) => (
                <div key={c.courseId} className="flex items-center justify-between rounded px-2 py-1 hover:bg-muted/50">
                  <div>
                    <span className="font-mono text-xs text-muted-foreground">{c.courseCode}</span>
                    <span className="ml-1 text-sm">{c.courseName}</span>
                  </div>
                  <div className="flex gap-1">
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-6 text-xs"
                      onClick={() => openAddAssignment(c.courseId, c.courseCode, c.courseName, false)}
                    >
                      Seçmeli
                    </Button>
                    <Button
                      size="sm"
                      className="h-6 text-xs"
                      onClick={() => openAddAssignment(c.courseId, c.courseCode, c.courseName, true)}
                    >
                      Zorunlu
                    </Button>
                  </div>
                </div>
              ))}
            </div>
            )}
          </div>
        </div>
        <DialogFooter><Button variant="outline" onClick={() => handleOpenChange(false)}>Kapat</Button></DialogFooter>
      </DialogContent>
    </Dialog>
    </>
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

      <CatDialog
        key={editTarget?.id ?? "new"}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        departmentId={departmentId}
        initial={editTarget}
        onSave={handleSave}
      />
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => { if (!v) setDeleteTarget(undefined); }}
        title="Kategoriyi sil"
        description={`"${deleteTarget?.name}" kategorisini silmek istediğinize emin misiniz?`}
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />
      {poolCat && (
        <CoursesPoolDialog
          catId={poolCat.id}
          catName={poolCat.name}
          open={!!poolCat}
          onOpenChange={(v) => { if (!v) setPoolCat(undefined); }}
        />
      )}
    </Card>
  );
}

/**
 * Admin tab: manage graduation categories per department.
 */
export function CategoriesTab() {
  const [departmentNameFilter, setDepartmentNameFilter] = useState("");

  const { data: departments, isLoading } = useQuery({ queryKey: ["departments"], queryFn: getDepartments });

  const filteredDepartments = useMemo(() => {
    return (departments ?? []).filter((d) => matchesTextFilter(d.name, departmentNameFilter));
  }, [departments, departmentNameFilter]);

  const hasActiveFilters = departmentNameFilter.trim() !== "";

  function clearFilters() {
    setDepartmentNameFilter("");
  }

  return (
    <section className="space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h2 className="text-lg font-semibold">Mezuniyet Kategorileri</h2>
          <p className="text-sm text-muted-foreground">
            Her bölüm için mezuniyet kategorilerini ve ders havuzlarını yönetin.
          </p>
        </div>
        {!isLoading && (departments?.length ?? 0) > 0 && (
          <Popover>
            <PopoverTrigger
              render={
                <Button
                  variant="outline"
                  size="sm"
                  className="transition-colors duration-150 shrink-0"
                  aria-pressed={hasActiveFilters}
                  aria-label="Bölümleri filtrele"
                />
              }
            >
              <Filter className="mr-1 h-4 w-4" />
              Filtre
              {hasActiveFilters && (
                <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">
                  1
                </Badge>
              )}
            </PopoverTrigger>
            <PopoverContent align="end" className="w-80 shadow-md">
              <PopoverHeader>
                <PopoverTitle>Bölümleri Filtrele</PopoverTitle>
              </PopoverHeader>
              <div className="flex flex-col gap-3">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="category-department-name-filter" className="text-sm font-medium text-muted-foreground">
                    Bölüm Adı
                  </label>
                  <Input
                    id="category-department-name-filter"
                    placeholder="Bölüm adına göre filtrele"
                    value={departmentNameFilter}
                    onChange={(e) => setDepartmentNameFilter(e.target.value)}
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
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
        </div>
      ) : departments?.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-8">Önce Bölümler sekmesinden bir bölüm ekleyin.</p>
      ) : filteredDepartments.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-8">Filtreye uygun bölüm bulunamadı.</p>
      ) : (
        <div className="space-y-3">
          {filteredDepartments.map((d) => (
            <DeptCategoryList key={d.id} departmentId={d.id} departmentName={d.name} />
          ))}
        </div>
      )}
    </section>
  );
}
