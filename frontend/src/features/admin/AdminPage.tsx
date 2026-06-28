import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { DepartmentsTab } from "./DepartmentsTab";
import { CoursesTab } from "./CoursesTab";
import { CategoriesTab } from "./CategoriesTab";
import { CurriculumEquivalenceRulesTab } from "./CurriculumEquivalenceRulesTab";

/**
 * Admin dashboard: CRUD management for departments, courses, categories,
 * and curriculum equivalence rules.
 */
export function AdminPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="text-2xl font-semibold">Admin Paneli</h1>

      <Tabs defaultValue="departments">
        <TabsList>
          <TabsTrigger value="departments">Bölümler</TabsTrigger>
          <TabsTrigger value="courses">Ders Kataloğu</TabsTrigger>
          <TabsTrigger value="categories">Mezuniyet Kategorileri</TabsTrigger>
          <TabsTrigger value="equivalences">Müfredat Değişikliği Kuralları</TabsTrigger>
        </TabsList>

        <TabsContent value="departments" className="pt-6">
          <DepartmentsTab />
        </TabsContent>

        <TabsContent value="courses" className="pt-6">
          <CoursesTab />
        </TabsContent>

        <TabsContent value="categories" className="pt-6">
          <CategoriesTab />
        </TabsContent>

        <TabsContent value="equivalences" className="pt-6">
          <CurriculumEquivalenceRulesTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}
