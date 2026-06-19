export interface Department {
  id: string;
  name: string;
  code: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Course {
  id: string;
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
}

export interface Category {
  id: string;
  name: string;
  departmentId: string;
  minCourseCount: number;
  minCredit?: number;
  minEcts?: number;
  description?: string;
}

export interface DepartmentCourse {
  courseId: string;
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
}

export interface CategoryCourse {
  courseId: string;
  courseCode: string;
  courseName: string;
  credit: number;
  ects: number;
  isMandatory: boolean;
}

export interface CreateDepartmentRequest {
  name: string;
  code: string;
  description?: string;
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
  minCredit?: number;
  minEcts?: number;
  description?: string;
}
