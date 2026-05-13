import {
  Car,
  Home,
  Laptop,
  Leaf,
  Shirt,
  Dumbbell,
  BookOpen,
  Baby,
  Briefcase,
  Tractor,
  MoreHorizontal,
  LucideIcon,
} from "lucide-react";

// Map category slugs to icons
const categoryIconMap: Record<string, LucideIcon> = {
  all: MoreHorizontal,
  vehicles: Car,
  "real-estate": Home,
  electronics: Laptop,
  "home-garden": Leaf,
  fashion: Shirt,
  sports: Dumbbell,
  "sports-hobbies": Dumbbell,
  books: BookOpen,
  "books-media": BookOpen,
  kids: Baby,
  "kids-baby": Baby,
  business: Briefcase,
  services: Briefcase,
  tools: Tractor,
  "agriculture-tools": Tractor,
};

export function getCategoryIcon(slug: string): LucideIcon {
  return categoryIconMap[slug.toLowerCase()] || MoreHorizontal;
}

