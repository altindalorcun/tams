export type AnalysisStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface TranscriptJobResponse {
  jobId: string;
  status: AnalysisStatus;
}

export interface CategoryResult {
  categoryId: string;
  categoryName: string;
  satisfied: boolean;
  requiredCredit: number;
  earnedCredit: number;
  requiredEcts: number;
  earnedEcts: number;
  requiredCourseCount: number;
  earnedCourseCount: number;
  missingMandatoryCourses: string[];
}

export interface AnalysisResult {
  id: string;
  jobId: string;
  studentNumber: string | null;
  departmentId: string;
  departmentName: string;
  status: AnalysisStatus;
  isEligible: boolean;
  gpa: number;
  totalCredit: number;
  totalEcts: number;
  createdAt: string;
  completedAt?: string;
  categoryResults: CategoryResult[];
  courses?: unknown[];
}

export interface AnalysisResultSummary {
  id: string;
  jobId: string;
  studentNumber: string | null;
  departmentId: string;
  departmentName: string;
  status: AnalysisStatus;
  isEligible: boolean;
  gpa: number;
  totalCredit: number;
  totalEcts: number;
  createdAt: string;
  completedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
