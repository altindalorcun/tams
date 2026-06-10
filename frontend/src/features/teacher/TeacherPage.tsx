import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { UploadSection } from "./UploadSection";
import { ResultCard, ResultCardSkeleton, HistoryTable } from "./ResultCard";
import { getResult, getResults } from "@/api/analysisApi";

/**
 * Teacher dashboard: upload transcripts, view results, browse history.
 */
export function TeacherPage() {
  const [activeTab, setActiveTab] = useState<"upload" | "history">("upload");
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const { data: resultDetail, isLoading: resultLoading } = useQuery({
    queryKey: ["result", selectedResultId],
    queryFn: () => getResult(selectedResultId!),
    enabled: !!selectedResultId,
  });

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ["results", page],
    queryFn: () => getResults(page, 20),
    enabled: activeTab === "history",
  });

  function handleResultReady(jobId: string) {
    setSelectedResultId(jobId);
  }

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="text-2xl font-semibold">Öğretmen Paneli</h1>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "upload" | "history")}>
        <TabsList>
          <TabsTrigger value="upload">Transkript Yükle</TabsTrigger>
          <TabsTrigger value="history">Geçmiş Analizler</TabsTrigger>
        </TabsList>

        <TabsContent value="upload" className="pt-6 space-y-6">
          <UploadSection onResultReady={handleResultReady} />

          {selectedResultId && (
            <section className="space-y-4">
              <h2 className="text-lg font-semibold">Analiz Sonucu</h2>
              {resultLoading || !resultDetail ? (
                <ResultCardSkeleton />
              ) : (
                <ResultCard result={resultDetail} />
              )}
            </section>
          )}
        </TabsContent>

        <TabsContent value="history" className="pt-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Geçmiş Analizler</h2>
            {history && history.totalPages > 1 && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="disabled:opacity-40 hover:text-foreground transition-colors duration-150"
                >
                  ← Önceki
                </button>
                <span>{page + 1} / {history.totalPages}</span>
                <button
                  onClick={() => setPage((p) => Math.min(history.totalPages - 1, p + 1))}
                  disabled={page >= history.totalPages - 1}
                  className="disabled:opacity-40 hover:text-foreground transition-colors duration-150"
                >
                  Sonraki →
                </button>
              </div>
            )}
          </div>

          <HistoryTable
            results={history?.content ?? []}
            isLoading={historyLoading}
            onSelect={(id) => {
              setSelectedResultId(id);
              setActiveTab("upload");
            }}
          />

          {selectedResultId && !historyLoading && (
            <section className="space-y-4 pt-4">
              <h2 className="text-lg font-semibold">Seçili Analiz Detayı</h2>
              {resultLoading || !resultDetail ? (
                <ResultCardSkeleton />
              ) : (
                <ResultCard result={resultDetail} />
              )}
            </section>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
