import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { UploadSection } from "./UploadSection";
import { ResultCard, ResultCardSkeleton } from "./ResultCard";
import { getResultByJobId } from "@/api/analysisApi";

/**
 * Teacher dashboard: upload transcripts and view the result of the latest upload.
 * Historical analysis data is available via the Analysis History page (/teacher/history).
 */
export function TeacherPage() {
  const [uploadJobId, setUploadJobId] = useState<string | null>(null);
  const [showResultDialog, setShowResultDialog] = useState(false);
  const [uploadSectionKey, setUploadSectionKey] = useState(0);

  const { data: uploadResult, isLoading: uploadResultLoading } = useQuery({
    queryKey: ["result-by-job", uploadJobId],
    queryFn: () => getResultByJobId(uploadJobId!),
    enabled: !!uploadJobId,
  });

  useEffect(() => {
    if (uploadResult) setShowResultDialog(true);
  }, [uploadResult]);

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
      <h1 className="text-2xl font-semibold">Transkript Yükle</h1>

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

      <UploadSection key={uploadSectionKey} onResultReady={handleResultReady} />
    </div>
  );
}
