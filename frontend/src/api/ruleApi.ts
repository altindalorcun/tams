import { axiosInstance } from "./axiosInstance";
import type {
  Department, Course, Category, DepartmentCourse, DepartmentCoursePoolResponse, CategoryCourse,
  CreateDepartmentRequest, CreateCourseRequest, CreateCategoryRequest,
} from "@/types";

// ── Departments ─────────────────────────────────────────────────────────────

export async function getDepartments(): Promise<Department[]> {
  const res = await axiosInstance.get<Department[]>("/api/v1/departments");
  return res.data;
}

export async function getDepartment(id: string): Promise<Department> {
  const res = await axiosInstance.get<Department>(`/api/v1/departments/${id}`);
  return res.data;
}

export async function createDepartment(data: CreateDepartmentRequest): Promise<Department> {
  const res = await axiosInstance.post<Department>("/api/v1/departments", data);
  return res.data;
}

export async function updateDepartment(id: string, data: CreateDepartmentRequest): Promise<Department> {
  const res = await axiosInstance.put<Department>(`/api/v1/departments/${id}`, data);
  return res.data;
}

export async function deleteDepartment(id: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/departments/${id}`);
}

export async function getDepartmentCourses(departmentId: string): Promise<DepartmentCourse[]> {
  const res = await axiosInstance.get<DepartmentCourse[]>(`/api/v1/departments/${departmentId}/courses`);
  return res.data;
}

export async function getDepartmentCoursePool(departmentId: string): Promise<DepartmentCoursePoolResponse> {
  const res = await axiosInstance.get<DepartmentCoursePoolResponse>(
    `/api/v1/departments/${departmentId}/course-pool`,
  );
  return res.data;
}

export async function addCourseToDepartment(departmentId: string, courseId: string): Promise<void> {
  await axiosInstance.post(`/api/v1/departments/${departmentId}/courses`, { courseId });
}

export async function removeCourseFromDepartment(departmentId: string, courseId: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/departments/${departmentId}/courses/${courseId}`);
}

// ── Courses ──────────────────────────────────────────────────────────────────

export async function getCourses(): Promise<Course[]> {
  const res = await axiosInstance.get<Course[]>("/api/v1/courses");
  return res.data;
}

export async function createCourse(data: CreateCourseRequest): Promise<Course> {
  const res = await axiosInstance.post<Course>("/api/v1/courses", data);
  return res.data;
}

export async function updateCourse(id: string, data: CreateCourseRequest): Promise<Course> {
  const res = await axiosInstance.put<Course>(`/api/v1/courses/${id}`, data);
  return res.data;
}

export async function deleteCourse(id: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/courses/${id}`);
}

// ── Categories ───────────────────────────────────────────────────────────────

export async function getCategories(departmentId: string): Promise<Category[]> {
  const res = await axiosInstance.get<Category[]>(`/api/v1/departments/${departmentId}/categories`);
  return res.data;
}

export async function createCategory(departmentId: string, data: CreateCategoryRequest): Promise<Category> {
  const res = await axiosInstance.post<Category>(`/api/v1/departments/${departmentId}/categories`, data);
  return res.data;
}

export async function updateCategory(departmentId: string, catId: string, data: CreateCategoryRequest): Promise<Category> {
  const res = await axiosInstance.put<Category>(`/api/v1/departments/${departmentId}/categories/${catId}`, data);
  return res.data;
}

export async function deleteCategory(departmentId: string, catId: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/departments/${departmentId}/categories/${catId}`);
}

export async function getCategoryCourses(catId: string): Promise<CategoryCourse[]> {
  const res = await axiosInstance.get<CategoryCourse[]>(`/api/v1/categories/${catId}/courses`);
  return res.data;
}

export async function addCourseToCategory(catId: string, courseId: string, isMandatory: boolean): Promise<void> {
  await axiosInstance.post(`/api/v1/categories/${catId}/courses`, { courseId, isMandatory });
}

export async function removeCourseFromCategory(catId: string, courseId: string): Promise<void> {
  await axiosInstance.delete(`/api/v1/categories/${catId}/courses/${courseId}`);
}
