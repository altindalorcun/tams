export interface Department {
  id: string;
  name: string;
  code: string;
}

export interface Course {
  id: string;
  courseCode: string;
  name: string;
  credits: number;
  ects: number;
}

export interface Category {
  id: string;
  name: string;
  departmentId: string;
  minCourseCount: number;
  minCredit?: number;
  minEcts?: number;
}

export interface DepartmentCourse {
  courseId: string;
  courseCode: string;
  name: string;
  credits: number;
  ects: number;
}

export interface CategoryCourse {
  courseId: string;
  courseCode: string;
  name: string;
  credits: number;
  ects: number;
  isMandatory: boolean;
}

export interface CreateDepartmentRequest {
  name: string;
  code: string;
}

export interface CreateCourseRequest {
  courseCode: string;
  name: string;
  credits: number;
  ects: number;
}

export interface CreateCategoryRequest {
  name: string;
  minCourseCount: number;
  minCredit?: number;
  minEcts?: number;
}
