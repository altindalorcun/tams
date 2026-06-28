import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, ChevronDown, ChevronRight, Filter } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage, FormDescription } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { DepartmentCoursePicker } from "@/components/DepartmentCoursePicker";
import { matchesTextFilter } from "@/lib/textFilter";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import {
  getDepartments,
  getCurriculumEquivalenceRules,
  createCurriculumEquivalenceRule,
  deleteCurriculumEquivalenceRule,
} from "@/api/ruleApi";
import type { CurriculumEquivalenceRule, CreateCurriculumEquivalenceRuleRequest, CurriculumEquivalenceRuleType } from "@/types";

const RULE_TYPE_LABELS: Record<CurriculumEquivalenceRuleType, string> = {
  PAIRWISE: "Bire Bir Eşdeğer (PAIRWISE)",
  GROUP_LEGACY_TO_REPLACEMENT: "Eski → Yeni (GROUP)",
  GROUP_REPLACEMENT_TO_LEGACY: "Yeni → Eski (GROUP)",
  GROUP_MUTUAL: "Çift Yönlü Grup (GROUP_MUTUAL)",
};

const RULE_TYPE_DESCRIPTIONS: Record<CurriculumEquivalenceRuleType, string> = {
  PAIRWISE:
    "Her eski ders[i] ile yeni ders[i] bire bir çift yönlü eşdeğerdir. Tarih kontrolü yapılmaz. Örnek: HAS222 ↔ MÜH103, HAS223 ↔ MÜH104.",
  GROUP_LEGACY_TO_REPLACEMENT:
    "Tüm eski dersler belirtilen tarihten önce geçildiyse yeni dersler geçilmiş sayılır. Örnek: FİZ103 + FİZ104 → FİZ117.",
  GROUP_REPLACEMENT_TO_LEGACY:
    "Tüm yeni dersler geçildiyse eski dersler geçilmiş sayılır.",
  GROUP_MUTUAL:
    "Her iki yön de geçerlidir. Örnek: BBM419 ↔ BBM479 + BBM480.",
};

const RULE_TYPES = [
  "PAIRWISE",
  "GROUP_LEGACY_TO_REPLACEMENT",
  "GROUP_REPLACEMENT_TO_LEGACY",
  "GROUP_MUTUAL",
] as const satisfies readonly CurriculumEquivalenceRuleType[];

const ruleSchema = z
  .object({
    ruleType: z.enum(RULE_TYPES),
    legacyCourseCodes: z.array(z.string()).min(1, "En az bir eski ders seçiniz"),
    replacementCourseCodes: z.array(z.string()).min(1, "En az bir yeni ders seçiniz"),
    effectiveFromYear: z.coerce.number().int().optional().nullable(),
    effectiveFromTerm: z.enum(["GUZ", "BAHAR"]).optional().nullable(),
  })
  .superRefine((data, ctx) => {
    if (data.ruleType === "PAIRWISE") {
      return;
    }
    if (data.effectiveFromYear == null || data.effectiveFromYear < 2000 || data.effectiveFromYear > 2100) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Geçerli bir yıl giriniz",
        path: ["effectiveFromYear"],
      });
    }
  });

type RuleFormValues = z.infer<typeof ruleSchema>;

const DEFAULT_VALUES: RuleFormValues = {
  ruleType: "PAIRWISE",
  legacyCourseCodes: [],
  replacementCourseCodes: [],
  effectiveFromYear: null,
  effectiveFromTerm: null,
};

function defaultValuesForRuleType(ruleType: CurriculumEquivalenceRuleType): RuleFormValues {
  return {
    ruleType,
    legacyCourseCodes: [],
    replacementCourseCodes: [],
    effectiveFromYear: ruleType === "PAIRWISE" ? null : undefined,
    effectiveFromTerm: null,
  };
}

interface AddDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  departmentId: string;
  onSave: (data: CreateCurriculumEquivalenceRuleRequest) => Promise<void>;
}

function AddEquivalenceRuleDialog({ open, onOpenChange, departmentId, onSave }: AddDialogProps) {
  const form = useForm<RuleFormValues>({
    resolver: zodResolver(ruleSchema),
    defaultValues: DEFAULT_VALUES,
  });

  const ruleType = form.watch("ruleType");
  const isGroup = ruleType !== "PAIRWISE";

  async function onSubmit(v: RuleFormValues) {
    await onSave({
      ruleType: v.ruleType,
      legacyCourseCodes: v.legacyCourseCodes,
      replacementCourseCodes: v.replacementCourseCodes,
      effectiveFromYear: v.ruleType === "PAIRWISE" ? null : (v.effectiveFromYear ?? null),
      effectiveFromTerm: v.ruleType === "PAIRWISE" ? null : (v.effectiveFromTerm ?? null),
    });
    form.reset(DEFAULT_VALUES);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg shadow-xl">
        <DialogHeader>
          <DialogTitle>Yeni Müfredat Değişikliği Kuralı</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">

            <FormField control={form.control} name="ruleType" render={({ field }) => (
              <FormItem>
                <FormLabel>Kural Tipi</FormLabel>
                <Select onValueChange={(val) => {
                  const nextType = val as CurriculumEquivalenceRuleType;
                  field.onChange(nextType);
                  form.reset(defaultValuesForRuleType(nextType));
                }} value={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Kural tipi seçiniz" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {RULE_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>{RULE_TYPE_LABELS[type]}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {ruleType && (
                  <FormDescription className="text-xs">
                    {RULE_TYPE_DESCRIPTIONS[ruleType]}
                  </FormDescription>
                )}
                <FormMessage />
              </FormItem>
            )} />

            <FormField control={form.control} name="legacyCourseCodes" render={({ field }) => (
              <FormItem>
                <FormLabel>Müfredattan Çıkarılan Dersler</FormLabel>
                <FormControl>
                  <DepartmentCoursePicker
                    mode="multiple"
                    departmentId={departmentId}
                    enabled={open}
                    triggerLabel="Eski dersleri seçiniz"
                    value={field.value}
                    onChange={field.onChange}
                  />
                </FormControl>
                <FormDescription className="text-xs">
                  Müfredattan kaldırılan (eski) ders kodları.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )} />

            <FormField control={form.control} name="replacementCourseCodes" render={({ field }) => (
              <FormItem>
                <FormLabel>Müfredata Eklenen Dersler</FormLabel>
                <FormControl>
                  <DepartmentCoursePicker
                    mode="multiple"
                    departmentId={departmentId}
                    enabled={open}
                    triggerLabel="Yeni dersleri seçiniz"
                    value={field.value}
                    onChange={field.onChange}
                  />
                </FormControl>
                <FormDescription className="text-xs">
                  Müfredata eklenen (yeni) ders kodları.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )} />

            {isGroup && (
              <div className="flex gap-3">
                <FormField control={form.control} name="effectiveFromYear" render={({ field }) => (
                  <FormItem className="flex-1">
                    <FormLabel>Etkin Yıl <span className="text-destructive">*</span></FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        placeholder="örn. 2019"
                        {...field}
                        value={field.value ?? ""}
                        onChange={(e) => field.onChange(e.target.value === "" ? null : Number(e.target.value))}
                      />
                    </FormControl>
                    <FormDescription className="text-xs">Değişikliğin başladığı akademik yılın ilk takvim yılı.</FormDescription>
                    <FormMessage />
                  </FormItem>
                )} />

                <FormField control={form.control} name="effectiveFromTerm" render={({ field }) => (
                  <FormItem className="w-36">
                    <FormLabel>Dönem</FormLabel>
                    <Select onValueChange={(val) => field.onChange(val === "none" ? null : val)} value={field.value ?? "none"}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Dönem" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="none">Belirtilmemiş</SelectItem>
                        <SelectItem value="GUZ">Güz</SelectItem>
                        <SelectItem value="BAHAR">Bahar</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>
            )}

            <DialogFooter className="pt-2">
              <Button variant="outline" type="button" onClick={() => { form.reset(DEFAULT_VALUES); onOpenChange(false); }}>
                İptal
              </Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>Ekle</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

function ruleTypeLabel(type: string) {
  return RULE_TYPE_LABELS[type as CurriculumEquivalenceRuleType] ?? type;
}

function ruleTypeBadgeVariant(type: string): "default" | "secondary" | "outline" | "destructive" {
  if (type === "PAIRWISE") return "secondary";
  if (type === "GROUP_MUTUAL") return "default";
  return "outline";
}

interface DeptRuleListProps {
  departmentId: string;
  departmentName: string;
}

function DeptEquivalenceRuleList({ departmentId, departmentName }: DeptRuleListProps) {
  const qc = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<CurriculumEquivalenceRule | undefined>();

  const { data: rules, isLoading } = useQuery({
    queryKey: ["curriculum-equivalence-rules", departmentId],
    queryFn: () => getCurriculumEquivalenceRules(departmentId),
    enabled: expanded,
  });

  const createMut = useMutation({
    mutationFn: (data: CreateCurriculumEquivalenceRuleRequest) =>
      createCurriculumEquivalenceRule(departmentId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["curriculum-equivalence-rules", departmentId] });
      toast.success("Müfredat değişikliği kuralı eklendi.");
    },
    onError: () => toast.error("Kural eklenemedi."),
  });

  const deleteMut = useMutation({
    mutationFn: (ruleId: string) => deleteCurriculumEquivalenceRule(departmentId, ruleId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["curriculum-equivalence-rules", departmentId] });
      toast.success("Kural silindi.");
      setDeleteTarget(undefined);
    },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  return (
    <Card className="shadow-sm">
      <CardHeader className="p-4 cursor-pointer" onClick={() => setExpanded((v) => !v)}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {expanded
              ? <ChevronDown className="h-4 w-4 text-muted-foreground" />
              : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
            <span className="font-medium">{departmentName}</span>
          </div>
          {expanded && (
            <Button size="sm" variant="outline" onClick={(e) => { e.stopPropagation(); setDialogOpen(true); }}>
              <Plus className="mr-1 h-4 w-4" />Kural Ekle
            </Button>
          )}
        </div>
      </CardHeader>

      {expanded && (
        <CardContent className="p-0">
          {isLoading ? (
            <div className="px-4 pb-4 space-y-2">
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-40">Kural Tipi</TableHead>
                  <TableHead>Eski Dersler</TableHead>
                  <TableHead>Yeni Dersler</TableHead>
                  <TableHead className="w-28">Etkin Yıl</TableHead>
                  <TableHead className="w-16 text-right">Sil</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rules?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground py-6">
                      Bu bölüme ait müfredat değişikliği kuralı yok.
                    </TableCell>
                  </TableRow>
                )}
                {rules?.map((rule) => (
                  <TableRow key={rule.id} className="hover:bg-muted/50 transition-colors duration-150">
                    <TableCell>
                      <Badge variant={ruleTypeBadgeVariant(rule.ruleType)} className="text-xs whitespace-nowrap">
                        {ruleTypeLabel(rule.ruleType)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {rule.legacyCourseCodes.map((code) => (
                          <Badge key={code} variant="secondary" className="font-mono text-xs">{code}</Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {rule.replacementCourseCodes.map((code) => (
                          <Badge key={code} variant="outline" className="font-mono text-xs">{code}</Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {rule.effectiveFromYear
                        ? `${rule.effectiveFromYear}${rule.effectiveFromTerm ? ` ${rule.effectiveFromTerm === "GUZ" ? "Güz" : "Bahar"}` : ""}`
                        : "—"}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-destructive hover:text-destructive"
                        onClick={() => setDeleteTarget(rule)}
                        aria-label="Kuralı sil"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      )}

      <AddEquivalenceRuleDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        departmentId={departmentId}
        onSave={async (data) => { await createMut.mutateAsync(data); }}
      />
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => { if (!v) setDeleteTarget(undefined); }}
        title="Müfredat değişikliği kuralını sil"
        description={
          deleteTarget
            ? `[${deleteTarget.legacyCourseCodes.join(", ")}] ↔ [${deleteTarget.replacementCourseCodes.join(", ")}] kuralını silmek istiyor musunuz?`
            : ""
        }
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />
    </Card>
  );
}

/**
 * Admin tab: manage curriculum equivalence rules per department.
 * These rules define which old curriculum courses are equivalent to new curriculum courses,
 * enabling the graduation engine to correctly evaluate transcripts that span curriculum changes.
 */
export function CurriculumEquivalenceRulesTab() {
  const [departmentNameFilter, setDepartmentNameFilter] = useState("");

  const { data: departments, isLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

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
          <h2 className="text-lg font-semibold">Müfredat Değişikliği Kuralları</h2>
          <p className="text-sm text-muted-foreground">
            Müfredattan kaldırılan ve eklenen derslerin eşdeğerliğini tanımlayın.
            Örnek: HAS222 ↔ MÜH103 (bire bir), BBM419 ↔ BBM479 + BBM480 (çift yönlü grup).
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
                <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">1</Badge>
              )}
            </PopoverTrigger>
            <PopoverContent align="end" className="w-80 shadow-md">
              <PopoverHeader>
                <PopoverTitle>Bölümleri Filtrele</PopoverTitle>
              </PopoverHeader>
              <div className="flex flex-col gap-3">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="equiv-department-filter" className="text-sm font-medium text-muted-foreground">
                    Bölüm Adı
                  </label>
                  <Input
                    id="equiv-department-filter"
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
        <p className="text-sm text-muted-foreground text-center py-8">
          Önce Bölümler sekmesinden bir bölüm ekleyin.
        </p>
      ) : filteredDepartments.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-8">
          Filtreye uygun bölüm bulunamadı.
        </p>
      ) : (
        <div className="space-y-3">
          {filteredDepartments.map((dept) => (
            <DeptEquivalenceRuleList
              key={dept.id}
              departmentId={dept.id}
              departmentName={dept.name}
            />
          ))}
        </div>
      )}
    </section>
  );
}
