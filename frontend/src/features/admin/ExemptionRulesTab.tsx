import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, ChevronDown, ChevronRight } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage, FormDescription } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { getDepartments, getExemptionRules, createExemptionRule, deleteExemptionRule } from "@/api/ruleApi";
import type { ExemptionRule, CreateExemptionRuleRequest } from "@/types";

const exemptionSchema = z.object({
  requiredCourseCodes: z.string().min(1, "En az bir ders kodu giriniz"),
  exemptedCourseCode: z.string().min(1, "Muaf tutulan ders kodu zorunludur").max(20),
});
type ExemptionFormValues = z.infer<typeof exemptionSchema>;

interface AddDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSave: (data: CreateExemptionRuleRequest) => Promise<void>;
}

function AddExemptionDialog({ open, onOpenChange, onSave }: AddDialogProps) {
  const form = useForm<ExemptionFormValues>({
    resolver: zodResolver(exemptionSchema),
    defaultValues: { requiredCourseCodes: "", exemptedCourseCode: "" },
  });

  async function onSubmit(v: ExemptionFormValues) {
    const codes = v.requiredCourseCodes
      .split(",")
      .map((c) => c.trim().toUpperCase())
      .filter(Boolean);
    await onSave({ requiredCourseCodes: codes, exemptedCourseCode: v.exemptedCourseCode.toUpperCase() });
    form.reset();
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm shadow-xl">
        <DialogHeader>
          <DialogTitle>Yeni Muafiyet Kuralı</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField control={form.control} name="requiredCourseCodes" render={({ field }) => (
              <FormItem>
                <FormLabel>Gerekli Ders Kodları</FormLabel>
                <FormControl>
                  <Input placeholder="örn. FIZ103, FIZ104" {...field} />
                </FormControl>
                <FormDescription className="text-xs">
                  Tüm bu dersler geçildiğinde muafiyet uygulanır. Virgülle ayırın.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="exemptedCourseCode" render={({ field }) => (
              <FormItem>
                <FormLabel>Muaf Tutulan Ders</FormLabel>
                <FormControl>
                  <Input placeholder="örn. FIZ117" {...field} />
                </FormControl>
                <FormDescription className="text-xs">
                  Bu ders, gerekli dersler tamamlandığında geçilmiş sayılır.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )} />
            <DialogFooter className="pt-2">
              <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>İptal</Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>Ekle</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

interface DeptExemptionListProps {
  departmentId: string;
  departmentName: string;
}

function DeptExemptionList({ departmentId, departmentName }: DeptExemptionListProps) {
  const qc = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ExemptionRule | undefined>();

  const { data: rules, isLoading } = useQuery({
    queryKey: ["exemption-rules", departmentId],
    queryFn: () => getExemptionRules(departmentId),
    enabled: expanded,
  });

  const createMut = useMutation({
    mutationFn: (data: CreateExemptionRuleRequest) => createExemptionRule(departmentId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["exemption-rules", departmentId] });
      toast.success("Muafiyet kuralı eklendi.");
    },
    onError: () => toast.error("Muafiyet kuralı eklenemedi."),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteExemptionRule(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["exemption-rules", departmentId] });
      toast.success("Muafiyet kuralı silindi.");
      setDeleteTarget(undefined);
    },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  return (
    <Card className="shadow-sm">
      <CardHeader className="p-4 cursor-pointer" onClick={() => setExpanded((v) => !v)}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {expanded ? <ChevronDown className="h-4 w-4 text-muted-foreground" /> : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
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
                  <TableHead>Gerekli Dersler</TableHead>
                  <TableHead>Muaf Tutulan Ders</TableHead>
                  <TableHead className="w-16 text-right">Sil</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rules?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={3} className="text-center text-muted-foreground py-6">
                      Bu bölüme ait muafiyet kuralı yok.
                    </TableCell>
                  </TableRow>
                )}
                {rules?.map((rule) => (
                  <TableRow key={rule.id} className="hover:bg-muted/50 transition-colors duration-150">
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {rule.requiredCourseCodes.map((code) => (
                          <Badge key={code} variant="secondary" className="font-mono text-xs">
                            {code}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className="font-mono text-xs">{rule.exemptedCourseCode}</Badge>
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

      <AddExemptionDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSave={async (data) => { await createMut.mutateAsync(data); }}
      />
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => { if (!v) setDeleteTarget(undefined); }}
        title="Muafiyet kuralını sil"
        description={
          deleteTarget
            ? `[${deleteTarget.requiredCourseCodes.join(", ")}] → ${deleteTarget.exemptedCourseCode} kuralını silmek istiyor musunuz?`
            : ""
        }
        onConfirm={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
        loading={deleteMut.isPending}
      />
    </Card>
  );
}

/**
 * Admin tab: manage exemption rules per department.
 * Exemption rules grant implicit credit for a course when all prerequisite courses are passed.
 */
export function ExemptionRulesTab() {
  const { data: departments, isLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold">Muafiyet Kuralları</h2>
      <p className="text-sm text-muted-foreground">
        Belirli dersleri geçen öğrencilere başka derslerden otomatik muafiyet tanımlayın.
        Örnek: FIZ103 + FIZ104 geçen öğrenci FIZ117'yi de geçmiş sayılır.
      </p>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}
        </div>
      ) : departments?.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-8">
          Önce Bölümler sekmesinden bir bölüm ekleyin.
        </p>
      ) : (
        <div className="space-y-3">
          {departments?.map((d) => (
            <DeptExemptionList key={d.id} departmentId={d.id} departmentName={d.name} />
          ))}
        </div>
      )}
    </section>
  );
}
