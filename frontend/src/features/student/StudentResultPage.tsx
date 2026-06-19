import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, XCircle, AlertTriangle, BookOpen } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { getMyResult } from "@/api/analysisApi";
import { cn } from "@/lib/utils";
import type { CategoryResult } from "@/types";

function CategoryProgressCard({ cat }: { cat: CategoryResult }) {
  const courseProgress = Math.min(100, (cat.earnedCourseCount / cat.requiredCourseCount) * 100);

  return (
    <Card className={cn("shadow-sm", !cat.satisfied && "border-destructive/40")}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-semibold">{cat.categoryName}</h3>
          <Badge
            variant={cat.satisfied ? "default" : "outline"}
            className={cn(!cat.satisfied && "border-destructive text-destructive")}
          >
            {cat.satisfied ? "Yeterli" : "Eksik"}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1">
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>Tamamlanan Ders</span>
            <span className="tabular-nums">{cat.earnedCourseCount} / {cat.requiredCourseCount}</span>
          </div>
          <Progress value={courseProgress} className="h-2" />
        </div>

        {cat.requiredCredit > 0 && (
          <div className="space-y-1">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Kredi</span>
              <span className="tabular-nums">{cat.earnedCredit} / {cat.requiredCredit}</span>
            </div>
            <Progress value={Math.min(100, (cat.earnedCredit / cat.requiredCredit) * 100)} className="h-2" />
          </div>
        )}

        {cat.requiredEcts > 0 && (
          <div className="space-y-1">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>AKTS</span>
              <span className="tabular-nums">{cat.earnedEcts} / {cat.requiredEcts}</span>
            </div>
            <Progress value={Math.min(100, (cat.earnedEcts / cat.requiredEcts) * 100)} className="h-2" />
          </div>
        )}

        {cat.missingMandatoryCourses.length > 0 && (
          <>
            <Separator />
            <div className="space-y-1.5">
              <p className="text-xs font-medium text-destructive flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" /> Eksik Zorunlu Dersler
              </p>
              {cat.missingMandatoryCourses.map((courseCode) => (
                <div key={courseCode} className="flex items-center gap-2 text-sm">
                  <AlertTriangle className="h-3.5 w-3.5 shrink-0 text-destructive" />
                  <span className="font-mono text-xs text-muted-foreground">{courseCode}</span>
                </div>
              ))}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * Student's personal graduation eligibility page.
 * Mobile-first, fully responsive layout.
 */
export function StudentResultPage() {
  const { data: result, isLoading, isError } = useQuery({
    queryKey: ["my-result"],
    queryFn: getMyResult,
  });

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-6 md:px-6 md:py-8 space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full rounded-lg" />
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-44 w-full rounded-lg" />)}
        </div>
      </div>
    );
  }

  if (isError || !result) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-6 md:px-6 md:py-8">
        <h1 className="text-2xl font-semibold mb-6">Mezuniyet Durumu</h1>
        <Alert>
          <BookOpen className="h-4 w-4" />
          <AlertTitle>Sonuç bulunamadı</AlertTitle>
          <AlertDescription>
            Henüz bir transkript analizi yapılmamış. Öğretmeninizden transkriptinizi sisteme yüklemesini isteyin.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const allMissingMandatory = result.categoryResults.flatMap((c) => c.missingMandatoryCourses);

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 md:px-6 md:py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-semibold">Mezuniyet Durumu</h1>
        <p className="text-sm text-muted-foreground mt-1">{result.departmentName}</p>
      </div>

      {/* Eligibility banner */}
      <div className={cn(
        "flex items-center gap-4 rounded-lg px-6 py-5",
        result.isEligible ? "bg-green-50 dark:bg-green-900/20" : "bg-red-50 dark:bg-red-900/20",
      )}>
        {result.isEligible
          ? <CheckCircle2 className="h-10 w-10 shrink-0 text-green-600 dark:text-green-400" />
          : <XCircle className="h-10 w-10 shrink-0 text-red-600 dark:text-red-500" />}
        <div>
          <p className={cn("text-xl font-semibold", result.isEligible ? "text-green-700 dark:text-green-400" : "text-red-700 dark:text-red-400")}>
            {result.isEligible ? "Mezuniyete Hak Kazandınız" : "Mezuniyete Hak Kazanamadınız"}
          </p>
          <p className="text-sm text-muted-foreground mt-0.5">
            Genel Akademik Ortalama: <strong>{result.gpa.toFixed(2)}</strong>
            {" · "}Analiz tarihi: {new Date(result.createdAt).toLocaleDateString("tr-TR")}
          </p>
        </div>
      </div>

      {/* Mandatory deficiencies summary */}
      {allMissingMandatory.length > 0 && (
        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertTitle>Alınmamış Zorunlu Dersler</AlertTitle>
          <AlertDescription>
            <ul className="mt-1 space-y-0.5">
              {allMissingMandatory.map((courseCode) => (
                <li key={courseCode} className="text-sm">
                  <span className="font-mono">{courseCode}</span>
                </li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}

      {/* Per-category breakdown */}
      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Kategori Bazlı İlerleme</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {result.categoryResults.map((cat) => (
            <CategoryProgressCard key={cat.categoryId} cat={cat} />
          ))}
        </div>
      </section>
    </div>
  );
}
