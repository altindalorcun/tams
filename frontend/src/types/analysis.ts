export type AnalysisStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface TranscriptJobResponse {
  jobId: string;
  status: AnalysisStatus;
}

export interface Deficiency {
  courseCode: string;
  courseName: string;
  isMandatory: boolean;
  reason: string;
}

export interface CategoryResult {
  categoryId: string;
  categoryName: string;
  isEligible: boolean;
  earnedCourseCount: number;
  requiredCourseCount: number;
  earnedCredits: number;
  requiredCredits?: number;
  earnedEcts: number;
  requiredEcts?: number;
  deficiencies: Deficiency[];
}

export interface AnalysisResult {
  id: string;
  jobId: string;
  studentRef: string;
  departmentId: string;
  departmentName: string;
  isEligible: boolean;
  gpa: number;
  createdAt: string;
  categoryResults: CategoryResult[];
}

export interface AnalysisResultSummary {
  id: string;
  jobId: string;
  studentRef: string;
  departmentName: string;
  isEligible: boolean;
  gpa: number;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
