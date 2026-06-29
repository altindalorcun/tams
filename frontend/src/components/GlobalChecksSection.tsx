import { AlertTriangle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";
import type { GlobalCheckResult } from "@/types";

function TotalEctsCheckCard({ check }: { check: GlobalCheckResult }) {
  const earned = check.earnedEcts ?? 0;
  const required = check.requiredMinEcts ?? 0;
  const progress = required > 0 ? Math.min(100, (earned / required) * 100) : 0;

  return (
    <Card className={cn("shadow-sm", !check.passed && "border-destructive/40")}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-semibold">Toplam AKTS Şartı</h3>
          <Badge
            variant={check.passed ? "default" : "outline"}
            className={cn(!check.passed && "border-destructive text-destructive")}
          >
            {check.passed ? "Yeterli" : "Eksik"}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <p className="text-sm text-muted-foreground">
          {check.passed
            ? `Toplam AKTS şartı karşılandı (${earned} / ${required})`
            : `Toplam AKTS yetersiz (${earned} / ${required})`}
        </p>
        <div className="space-y-1">
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>AKTS</span>
            <span className="tabular-nums">{earned} / {required}</span>
          </div>
          <Progress value={progress} className="h-2" />
        </div>
      </CardContent>
    </Card>
  );
}

function FailGradeCheckCard({ check }: { check: GlobalCheckResult }) {
  return (
    <Card className={cn("shadow-sm", !check.passed && "border-destructive/40")}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-semibold">F Notu Engeli</h3>
          <Badge
            variant={check.passed ? "default" : "outline"}
            className={cn(!check.passed && "border-destructive text-destructive")}
          >
            {check.passed ? "Uygun" : "Engellendi"}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-2">
        <p className="text-sm text-muted-foreground">
          {check.passed
            ? "Transkriptte başarısız ders bulunmuyor."
            : "Bölümde F notu engeli vardır; transkriptte başarısız ders bulundu."}
        </p>
        {check.failedCourseCodes.length > 0 && (
          <div className="space-y-1">
            <p className="text-xs font-medium text-destructive flex items-center gap-1">
              <AlertTriangle className="h-3 w-3" /> Başarısız Dersler
            </p>
            {check.failedCourseCodes.map((courseCode) => (
              <div key={courseCode} className="text-xs font-mono text-muted-foreground">
                {courseCode}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

interface GlobalChecksSectionProps {
  globalCheckResults: GlobalCheckResult[];
}

/**
 * Displays department-level global graduation rules such as total ECTS minimum and fail-grade block.
 */
export function GlobalChecksSection({ globalCheckResults }: GlobalChecksSectionProps) {
  if (globalCheckResults.length === 0) {
    return null;
  }

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold">Genel Bölüm Kuralları</h2>
      <div className="grid gap-4 sm:grid-cols-2">
        {globalCheckResults.map((check) => {
          if (check.checkType === "TOTAL_ECTS") {
            return <TotalEctsCheckCard key={check.checkType} check={check} />;
          }
          return <FailGradeCheckCard key={check.checkType} check={check} />;
        })}
      </div>
    </section>
  );
}
