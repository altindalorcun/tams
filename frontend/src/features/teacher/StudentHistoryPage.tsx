import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ResultCard, ResultCardSkeleton, HistoryTable } from "./ResultCard";
import { getResult, getResults } from "@/api/analysisApi";

/**
 * Displays the full analysis history for all students, with pagination and inline detail view.
 */
export function StudentHistoryPage() {
  const [page, setPage] = useState(0);
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ["results", page],
    queryFn: () => getResults(page, 20),
  });

  const { data: selectedResult, isLoading: selectedResultLoading } = useQuery({
    queryKey: ["result", selectedResultId],
    queryFn: () => getResult(selectedResultId!),
    enabled: !!selectedResultId,
  });

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="text-2xl font-semibold">Analiz Geçmişi</h1>

      <section className="space-y-4">
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
          onSelect={(id) => setSelectedResultId(id)}
        />
      </section>

      {selectedResultId && (
        <section className="space-y-4 pt-4">
          <h2 className="text-lg font-semibold">Seçili Analiz Detayı</h2>
          {selectedResultLoading || !selectedResult ? (
            <ResultCardSkeleton />
          ) : (
            <ResultCard result={selectedResult} />
          )}
        </section>
      )}
    </div>
  );
}
