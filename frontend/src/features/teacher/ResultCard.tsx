import { CheckCircle2, XCircle, AlertTriangle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import type { AnalysisResult, AnalysisResultSummary, CategoryResult } from "@/types";

function EligibilityBanner({ isEligible, gpa }: { isEligible: boolean; gpa: number }) {
  return (
    <div className={cn(
      "flex items-center gap-4 rounded-lg px-6 py-5",
      isEligible ? "bg-green-50 dark:bg-green-900/20" : "bg-red-50 dark:bg-red-900/20",
    )}>
      {isEligible
        ? <CheckCircle2 className="h-8 w-8 shrink-0 text-green-600 dark:text-green-400" />
        : <XCircle className="h-8 w-8 shrink-0 text-red-600 dark:text-red-500" />}
      <div>
        <p className={cn("text-lg font-semibold", isEligible ? "text-green-700 dark:text-green-400" : "text-red-700 dark:text-red-400")}>
          {isEligible ? "Mezuniyete Hak Kazandı" : "Mezuniyete Hak Kazanamadı"}
        </p>
        <p className="text-sm text-muted-foreground mt-0.5">Genel Akademik Ortalama: {gpa.toFixed(2)}</p>
      </div>
    </div>
  );
}

function CategoryCard({ cat }: { cat: CategoryResult }) {
  const courseProgress = Math.min(100, (cat.earnedCourseCount / cat.requiredCourseCount) * 100);
  const creditProgress = cat.requiredCredit > 0
    ? Math.min(100, (cat.earnedCredit / cat.requiredCredit) * 100)
    : null;
  const ectsProgress = cat.requiredEcts > 0
    ? Math.min(100, (cat.earnedEcts / cat.requiredEcts) * 100)
    : null;

  return (
    <Card className={cn("shadow-sm", !cat.satisfied && "border-destructive/40")}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">{cat.categoryName}</h3>
          <Badge variant={cat.satisfied ? "default" : "outline"} className={cn(!cat.satisfied && "border-destructive text-destructive")}>
            {cat.satisfied ? "Yeterli" : "Eksik"}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1">
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Ders sayısı</span>
            <span>{cat.earnedCourseCount} / {cat.requiredCourseCount}</span>
          </div>
          <Progress value={courseProgress} className="h-1.5" />
        </div>

        {creditProgress !== null && (
          <div className="space-y-1">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Kredi</span>
              <span>{cat.earnedCredit} / {cat.requiredCredit}</span>
            </div>
            <Progress value={creditProgress} className="h-1.5" />
          </div>
        )}

        {ectsProgress !== null && (
          <div className="space-y-1">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>AKTS</span>
              <span>{cat.earnedEcts} / {cat.requiredEcts}</span>
            </div>
            <Progress value={ectsProgress} className="h-1.5" />
          </div>
        )}

        {cat.missingMandatoryCourses.length > 0 && (
          <>
            <Separator />
            <div className="space-y-1">
              <p className="text-xs font-medium text-destructive flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" /> Eksik Zorunlu Dersler
              </p>
              {cat.missingMandatoryCourses.map((courseCode) => (
                <div key={courseCode} className="text-xs text-muted-foreground">
                  <span className="font-mono">{courseCode}</span>
                </div>
              ))}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}

interface ResultCardProps {
  result: AnalysisResult;
  isLoading?: boolean;
}

/**
 * Displays a full analysis result with eligibility, category breakdown, and missing mandatory courses.
 */
export function ResultCard({ result, isLoading }: ResultCardProps) {
  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-20 w-full rounded-lg" />
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-40 w-full rounded-lg" />)}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <EligibilityBanner isEligible={result.isEligible} gpa={result.gpa} />

      <div>
        <h2 className="text-lg font-semibold mb-4">Kategori Detayları</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {result.categoryResults.map((cat) => (
            <CategoryCard key={cat.categoryId} cat={cat} />
          ))}
        </div>
      </div>
    </div>
  );
}

export function ResultCardSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-20 w-full rounded-lg" />
      <div className="grid gap-4 sm:grid-cols-2">
        {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-40 w-full rounded-lg" />)}
      </div>
    </div>
  );
}

export function HistoryTable({ results, isLoading, onSelect }: {
  results: AnalysisResultSummary[];
  isLoading: boolean;
  onSelect: (id: string) => void;
}) {
  if (isLoading) {
    return <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>;
  }

  return (
    <div className="rounded-lg border shadow-sm overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Öğrenci No</TableHead>
            <TableHead>Bölüm</TableHead>
            <TableHead className="text-right">GNO</TableHead>
            <TableHead>Durum</TableHead>
            <TableHead>Tarih</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {results.length === 0 && (
            <TableRow>
              <TableCell colSpan={5} className="text-center text-muted-foreground py-8">Henüz analiz yapılmamış.</TableCell>
            </TableRow>
          )}
          {results.map((r) => (
            <TableRow
              key={r.id}
              className="hover:bg-muted/50 transition-colors duration-150 cursor-pointer"
              onClick={() => onSelect(r.id)}
            >
              <TableCell className="font-mono text-xs">{r.studentNumber ?? "—"}</TableCell>
              <TableCell>{r.departmentName}</TableCell>
              <TableCell className="text-right tabular-nums">{r.gpa.toFixed(2)}</TableCell>
              <TableCell>
                <Badge variant={r.isEligible ? "default" : "outline"} className={cn(!r.isEligible && "border-destructive text-destructive")}>
                  {r.isEligible ? "Uygun" : "Eksik"}
                </Badge>
              </TableCell>
              <TableCell className="text-muted-foreground text-sm">
                {new Date(r.createdAt).toLocaleDateString("tr-TR")}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
