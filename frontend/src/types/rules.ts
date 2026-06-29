export type EnrollmentTerm = "GUZ" | "BAHAR";

export type CurriculumEquivalenceRuleType =
  | "PAIRWISE"
  | "GROUP_LEGACY_TO_REPLACEMENT"
  | "GROUP_REPLACEMENT_TO_LEGACY"
  | "GROUP_MUTUAL";

export interface CurriculumEquivalenceRule {
  id: string;
  ruleType: CurriculumEquivalenceRuleType;
  legacyCourseCodes: string[];
  replacementCourseCodes: string[];
  effectiveFromYear?: number | null;
  effectiveFromTerm?: "GUZ" | "BAHAR" | null;
}

export interface CreateCurriculumEquivalenceRuleRequest {
  ruleType: CurriculumEquivalenceRuleType;
  legacyCourseCodes: string[];
  replacementCourseCodes: string[];
  effectiveFromYear?: number | null;
  effectiveFromTerm?: "GUZ" | "BAHAR" | null;
}

export interface Department {
  id: string;
  name: string;
  code: string;
  description?: string;
  minTotalEcts?: number | null;
  blockOnAnyFGrade: boolean;
  createdAt?: string;
  updatedAt?: string;
  curriculumEquivalenceRules?: CurriculumEquivalenceRule[];
}

export interface Course {
  id: string;
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
  departmentIds?: string[];
}

export interface PrefixLimit {
  id: string;
  courseCodePrefix: string;
  maxCount: number;
}

export interface Category {
  id: string;
  name: string;
  departmentId: string;
  minCourseCount: number;
  minCredit: number;
  minEcts: number;
  description?: string;
  appliesFromYear?: number | null;
  appliesToYear?: number | null;
  conditionCourseCodes?: string[] | null;
  minCourseCountIfMet?: number | null;
  minEctsIfMet?: number | null;
  prefixLimits?: PrefixLimit[];
}

export interface DepartmentCourse {
  courseId: string;
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
}

export interface DepartmentCoursePoolResponse {
  assignedCourses: DepartmentCourse[];
  availableCourses: Course[];
}

export interface CategoryCourse {
  courseId: string;
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
  isMandatory: boolean;
  appliesFromYear?: number | null;
  appliesFromTerm?: EnrollmentTerm | null;
  appliesToYear?: number | null;
  appliesToTerm?: EnrollmentTerm | null;
}

export interface CategoryCourseRequest {
  courseId: string;
  isMandatory: boolean;
  appliesFromYear?: number | null;
  appliesFromTerm?: EnrollmentTerm | null;
  appliesToYear?: number | null;
  appliesToTerm?: EnrollmentTerm | null;
}

export interface UpdateCategoryCourseRequest {
  isMandatory: boolean;
  appliesFromYear?: number | null;
  appliesFromTerm?: EnrollmentTerm | null;
  appliesToYear?: number | null;
  appliesToTerm?: EnrollmentTerm | null;
}

export interface CreateDepartmentRequest {
  name: string;
  code: string;
  description?: string;
  minTotalEcts?: number | null;
  blockOnAnyFGrade?: boolean;
}

export interface UpdateDepartmentRequest {
  name: string;
  code: string;
  description?: string;
  minTotalEcts?: number | null;
  blockOnAnyFGrade?: boolean;
}

export interface CreateCourseRequest {
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
}

export interface CreateCategoryRequest {
  name: string;
  minCourseCount: number;
  minCredit: number;
  minEcts: number;
  description?: string;
  appliesFromYear?: number | null;
  appliesToYear?: number | null;
  conditionCourseCodes?: string[] | null;
  minCourseCountIfMet?: number | null;
  minEctsIfMet?: number | null;
  prefixLimits?: CreatePrefixLimitRequest[];
}


export interface CreatePrefixLimitRequest {
  courseCodePrefix: string;
  maxCount: number;
}
