import { axiosInstance } from "./axiosInstance";
import type { AnalysisResult, AnalysisResultSummary, PageResponse, TranscriptJobResponse } from "@/types";

/**
 * Upload a transcript PDF for analysis.
 * Returns a job ID to poll for status.
 */
export async function uploadTranscript(file: File, departmentId: string): Promise<TranscriptJobResponse> {
  const form = new FormData();
  form.append("file", file);
  const res = await axiosInstance.post<TranscriptJobResponse>(
    `/api/v1/transcripts?departmentId=${departmentId}`,
    form,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return res.data;
}

/**
 * Poll the status of a transcript analysis job.
 */
export async function getJobStatus(jobId: string): Promise<TranscriptJobResponse> {
  const res = await axiosInstance.get<TranscriptJobResponse>(`/api/v1/transcripts/${jobId}/status`);
  return res.data;
}

/**
 * Get all results for the authenticated teacher (paginated).
 */
export async function getResults(
  page = 0,
  size = 20,
  studentNumber?: string,
): Promise<PageResponse<AnalysisResultSummary>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (studentNumber) params.set("studentNumber", studentNumber);
  const res = await axiosInstance.get<PageResponse<AnalysisResultSummary>>(
    `/api/v1/results?${params}`,
  );
  return res.data;
}

/**
 * Get full analysis result with category breakdowns.
 */
export async function getResult(id: string): Promise<AnalysisResult> {
  const res = await axiosInstance.get<AnalysisResult>(`/api/v1/results/${id}`);
  return res.data;
}

/**
 * Student: get own latest result.
 * The backend resolves the student identity from the JWT studentNumber claim.
 */
export async function getMyResult(): Promise<AnalysisResult> {
  const res = await axiosInstance.get<AnalysisResult>("/api/v1/results/me");
  return res.data;
}

/**
 * Teacher: get full analysis result by job ID.
 * Use this after uploading a transcript — the jobId comes from the upload response.
 */
export async function getResultByJobId(jobId: string): Promise<AnalysisResult> {
  const res = await axiosInstance.get<AnalysisResult>(`/api/v1/results/by-job/${jobId}`);
  return res.data;
}
