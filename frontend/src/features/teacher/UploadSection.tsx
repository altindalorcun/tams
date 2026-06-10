import { useState, useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { useQuery, useMutation } from "@tanstack/react-query";
import { Upload, FileText, Loader2, CheckCircle2, XCircle } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { getDepartments } from "@/api/ruleApi";
import { uploadTranscript, getJobStatus } from "@/api/analysisApi";
import { cn } from "@/lib/utils";
import type { AnalysisStatus } from "@/types";

const POLL_INTERVAL_MS = 3000;
const MAX_FILE_SIZE_MB = 10;

interface UploadSectionProps {
  onResultReady: (resultId: string) => void;
}

/**
 * PDF upload area with drag-and-drop, department selector, and job status polling.
 */
export function UploadSection({ onResultReady }: UploadSectionProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [departmentId, setDepartmentId] = useState<string>("");
  const [jobId, setJobId] = useState<string | null>(null);
  const [jobStatus, setJobStatus] = useState<AnalysisStatus | null>(null);

  const { data: departments, isLoading: depsLoading } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  const uploadMut = useMutation({
    mutationFn: () => uploadTranscript(selectedFile!, departmentId),
    onSuccess: (res) => {
      setJobId(res.jobId);
      setJobStatus("PENDING");
      toast.info("Transkript yüklendi, analiz başlıyor…");
    },
    onError: () => toast.error("Yükleme başarısız. Lütfen tekrar deneyin."),
  });

  useQuery({
    queryKey: ["job-status", jobId],
    queryFn: () => getJobStatus(jobId!),
    enabled: !!jobId && jobStatus === "PENDING",
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "PENDING" ? POLL_INTERVAL_MS : false;
    },
    select: (data) => data,
    gcTime: 0,
    meta: {
      onSuccess: (data: { status: AnalysisStatus; jobId: string }) => {
        if (data.status !== "PENDING") {
          setJobStatus(data.status);
          if (data.status === "COMPLETED") {
            toast.success("Analiz tamamlandı.");
            onResultReady(data.jobId);
          } else {
            toast.error("Analiz başarısız oldu.");
          }
        }
      },
    },
  });

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (!file) return;
    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      toast.error(`Dosya boyutu ${MAX_FILE_SIZE_MB} MB'ı aşamaz.`);
      return;
    }
    setSelectedFile(file);
    setJobId(null);
    setJobStatus(null);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { "application/pdf": [".pdf"] },
    maxFiles: 1,
    disabled: uploadMut.isPending || jobStatus === "PENDING",
  });

  const canUpload = !!selectedFile && !!departmentId && !uploadMut.isPending && jobStatus !== "PENDING";

  return (
    <Card className="shadow-sm">
      <CardHeader className="pb-2">
        <h2 className="text-lg font-semibold">Transkript Yükle</h2>
        <p className="text-sm text-muted-foreground">
          PDF formatında transkript yükleyin. Maksimum dosya boyutu {MAX_FILE_SIZE_MB} MB.
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Department selector */}
        <div className="space-y-1.5">
          <label htmlFor="department-select" className="text-sm font-medium">
            Bölüm Seçin
          </label>
          {depsLoading ? (
            <Skeleton className="h-9 w-full" />
          ) : (
            <select
              id="department-select"
              value={departmentId}
              onChange={(e) => setDepartmentId(e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring transition-colors duration-150"
              disabled={uploadMut.isPending || jobStatus === "PENDING"}
            >
              <option value="">— Bölüm seçin —</option>
              {departments?.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          )}
        </div>

        {/* Dropzone */}
        <div
          {...getRootProps()}
          className={cn(
            "flex flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed px-6 py-10 cursor-pointer transition-colors duration-150",
            isDragActive ? "border-primary bg-primary/5" : "border-border hover:border-primary/50 hover:bg-muted/30",
            (uploadMut.isPending || jobStatus === "PENDING") && "pointer-events-none opacity-60",
          )}
        >
          <input {...getInputProps()} />
          {selectedFile ? (
            <>
              <FileText className="h-8 w-8 text-primary" />
              <div className="text-center">
                <p className="text-sm font-medium">{selectedFile.name}</p>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {(selectedFile.size / 1024).toFixed(0)} KB — değiştirmek için tıklayın veya sürükleyin
                </p>
              </div>
            </>
          ) : (
            <>
              <Upload className="h-8 w-8 text-muted-foreground" />
              <div className="text-center">
                <p className="text-sm font-medium">PDF'i buraya sürükleyin</p>
                <p className="text-xs text-muted-foreground mt-0.5">veya seçmek için tıklayın</p>
              </div>
            </>
          )}
        </div>

        {/* Status indicator */}
        {jobStatus && (
          <div className={cn("flex items-center gap-2 rounded-md px-3 py-2 text-sm", {
            "bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:text-amber-400": jobStatus === "PENDING",
            "bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400": jobStatus === "COMPLETED",
            "bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400": jobStatus === "FAILED",
          })}>
            {jobStatus === "PENDING" && <Loader2 className="h-4 w-4 animate-spin" />}
            {jobStatus === "COMPLETED" && <CheckCircle2 className="h-4 w-4" />}
            {jobStatus === "FAILED" && <XCircle className="h-4 w-4" />}
            {jobStatus === "PENDING" ? "Analiz devam ediyor…" : jobStatus === "COMPLETED" ? "Analiz tamamlandı." : "Analiz başarısız oldu."}
          </div>
        )}

        <Button
          className="w-full transition-colors duration-150"
          disabled={!canUpload}
          onClick={() => uploadMut.mutate()}
        >
          {uploadMut.isPending ? (
            <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Yükleniyor…</>
          ) : (
            <><Upload className="mr-2 h-4 w-4" />Analizi Başlat</>
          )}
        </Button>
      </CardContent>
    </Card>
  );
}

export function StatusBadge({ status }: { status: AnalysisStatus }) {
  const map: Record<AnalysisStatus, { label: string; variant: "default" | "outline" | "secondary" }> = {
    PENDING: { label: "Beklemede", variant: "secondary" },
    COMPLETED: { label: "Tamamlandı", variant: "default" },
    FAILED: { label: "Başarısız", variant: "outline" },
  };
  const { label, variant } = map[status];
  return <Badge variant={variant}>{label}</Badge>;
}
