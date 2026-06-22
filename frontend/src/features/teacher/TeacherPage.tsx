import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { UploadSection } from "./UploadSection";
import { ResultCard, ResultCardSkeleton, HistoryTable } from "./ResultCard";
import { getResult, getResults, getResultByJobId } from "@/api/analysisApi";

/**
 * Teacher dashboard: upload transcripts, view results, browse history.
 */
export function TeacherPage() {
  const [activeTab, setActiveTab] = useState<"upload" | "history">("upload");
  const [uploadJobId, setUploadJobId] = useState<string | null>(null);
  const [showResultDialog, setShowResultDialog] = useState(false);
  const [uploadSectionKey, setUploadSectionKey] = useState(0);
  const [historyResultId, setHistoryResultId] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const { data: uploadResult, isLoading: uploadResultLoading } = useQuery({
    queryKey: ["result-by-job", uploadJobId],
    queryFn: () => getResultByJobId(uploadJobId!),
    enabled: !!uploadJobId,
  });

  useEffect(() => {
    if (uploadResult) setShowResultDialog(true);
  }, [uploadResult]);

  const { data: historyResult, isLoading: historyResultLoading } = useQuery({
    queryKey: ["result", historyResultId],
    queryFn: () => getResult(historyResultId!),
    enabled: !!historyResultId,
  });

  const { data: history, isLoading: historyLoading } = useQuery({
    queryKey: ["results", page],
    queryFn: () => getResults(page, 20),
    enabled: activeTab === "history",
  });

  function handleResultReady(jobId: string) {
    setUploadJobId(jobId);
  }

  function handleDialogClose() {
    setShowResultDialog(false);
    setUploadJobId(null);
    setUploadSectionKey((k) => k + 1);
  }

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="text-2xl font-semibold">Öğretmen Paneli</h1>

      <Dialog open={showResultDialog} onOpenChange={(open) => { if (!open) handleDialogClose(); }}>
        <DialogContent className="max-w-3xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-lg font-semibold">Analiz Tamamlandı</DialogTitle>
          </DialogHeader>

          <div className="py-2">
            {uploadResultLoading || !uploadResult ? (
              <ResultCardSkeleton />
            ) : (
              <ResultCard result={uploadResult} />
            )}
          </div>

          <DialogFooter>
            <Button onClick={handleDialogClose} disabled={uploadResultLoading || !uploadResult}>
              Tamam
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "upload" | "history")}>
        <TabsList>
          <TabsTrigger value="upload">Transkript Yükle</TabsTrigger>
          <TabsTrigger value="history">Geçmiş Analizler</TabsTrigger>
        </TabsList>

        <TabsContent value="upload" className="pt-6">
          <UploadSection key={uploadSectionKey} onResultReady={handleResultReady} />
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
              setHistoryResultId(id);
              setActiveTab("upload");
            }}
          />

          {historyResultId && !historyLoading && (
            <section className="space-y-4 pt-4">
              <h2 className="text-lg font-semibold">Seçili Analiz Detayı</h2>
              {historyResultLoading || !historyResult ? (
                <ResultCardSkeleton />
              ) : (
                <ResultCard result={historyResult} />
              )}
            </section>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
