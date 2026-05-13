import { useState } from 'react';
import {
  Search,
  Heart,
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
  User,
  Clock
} from 'lucide-react';

const categories = [
  { id: 'all', name: 'All Categories', icon: MoreHorizontal },
  { id: 'vehicles', name: 'Vehicles', icon: Car },
  { id: 'real-estate', name: 'Real Estate', icon: Home },
  { id: 'electronics', name: 'Electronics', icon: Laptop },
  { id: 'home-garden', name: 'Home & Garden', icon: Leaf },
  { id: 'fashion', name: 'Fashion', icon: Shirt },
  { id: 'sports', name: 'Sports & Hobbies', icon: Dumbbell },
  { id: 'books', name: 'Books & Media', icon: BookOpen },
  { id: 'kids', name: 'Kids & Baby', icon: Baby },
  { id: 'business', name: 'Business & Services', icon: Briefcase },
  { id: 'agriculture', name: 'Agriculture & Tools', icon: Tractor },
];

const items = [
  {
    id: 1,
    title: 'Canon EOS 80D Camera',
    category: 'Electronics',
    owner: 'John D.',
    time: '2 hours ago',
    image: 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 2,
    title: 'Mountain Bike Trek Marlin 5',
    category: 'Sports & Hobbies',
    owner: 'Sarah M.',
    time: '5 hours ago',
    image: 'https://images.unsplash.com/photo-1576435728678-68d0fbf94e91?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 3,
    title: 'Sofa in excellent condition',
    category: 'Home & Garden',
    owner: 'Mike R.',
    time: '1 day ago',
    image: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 4,
    title: 'MacBook Pro 13" 2020',
    category: 'Electronics',
    owner: 'Emily K.',
    time: '3 hours ago',
    image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 5,
    title: 'Electric Guitar Ibanez',
    category: 'Sports & Hobbies',
    owner: 'Tom L.',
    time: '6 hours ago',
    image: 'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 6,
    title: 'iPhone 12 64GB',
    category: 'Electronics',
    owner: 'Lisa P.',
    time: '4 hours ago',
    image: 'https://images.unsplash.com/photo-1592286927505-2e5de6dd6aa5?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 7,
    title: 'DeWalt Cordless Drill',
    category: 'Agriculture & Tools',
    owner: 'David W.',
    time: '12 hours ago',
    image: 'https://images.unsplash.com/photo-1504148455328-c376907d081c?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 8,
    title: '2-Bedroom Apartment',
    category: 'Real Estate',
    owner: 'Anna B.',
    time: '2 days ago',
    image: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 9,
    title: 'Wooden Dining Table',
    category: 'Home & Garden',
    owner: 'Chris H.',
    time: '8 hours ago',
    image: 'https://images.unsplash.com/photo-1617806118233-18e1de247200?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 10,
    title: 'Harry Potter Books Set',
    category: 'Books & Media',
    owner: 'Rachel T.',
    time: '1 day ago',
    image: 'https://images.unsplash.com/photo-1621351183012-e2f9972dd9bf?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 11,
    title: 'Vintage Vinyl Records',
    category: 'Books & Media',
    owner: 'Peter S.',
    time: '5 hours ago',
    image: 'https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 12,
    title: 'Gaming Chair RGB',
    category: 'Home & Garden',
    owner: 'Alex N.',
    time: '7 hours ago',
    image: 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 13,
    title: 'Kids Bicycle 20"',
    category: 'Kids & Baby',
    owner: 'Maria G.',
    time: '10 hours ago',
    image: 'https://images.unsplash.com/photo-1548011722-88fcbfea1e7c?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 14,
    title: 'Designer Leather Jacket',
    category: 'Fashion',
    owner: 'Steve J.',
    time: '1 day ago',
    image: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400&h=300&fit=crop',
    status: 'Active'
  },
  {
    id: 15,
    title: 'Professional Coffee Machine',
    category: 'Home & Garden',
    owner: 'Nina F.',
    time: '3 hours ago',
    image: 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=400&h=300&fit=crop',
    status: 'Active'
  }
];

export default function App() {
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [favorites, setFavorites] = useState<Set<number>>(new Set());

  const toggleFavorite = (itemId: number) => {
    setFavorites(prev => {
      const newFavorites = new Set(prev);
      if (newFavorites.has(itemId)) {
        newFavorites.delete(itemId);
      } else {
        newFavorites.add(itemId);
      }
      return newFavorites;
    });
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Top Navigation */}
      <nav className="bg-white border-b border-border sticky top-0 z-50">
        <div className="max-w-[1600px] mx-auto px-6 py-4 flex items-center gap-8">
          {/* Logo */}
          <div className="flex items-center gap-2 shrink-0">
            <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
              <span className="text-white text-lg">⇄</span>
            </div>
            <span className="text-xl font-semibold text-foreground">Barter Platform</span>
          </div>

          {/* Search Bar */}
          <div className="flex-1 max-w-2xl">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search for items to trade..."
                className="w-full pl-12 pr-4 py-3 bg-input-background rounded-lg border border-transparent focus:border-primary focus:outline-none"
              />
            </div>
          </div>

          {/* Nav Links */}
          <div className="flex items-center gap-6 shrink-0">
            <button className="text-foreground hover:text-primary transition-colors">
              Categories
            </button>
            <button className="text-foreground hover:text-primary transition-colors">
              How it works
            </button>
            <button className="px-4 py-2 text-foreground hover:text-primary transition-colors">
              Login
            </button>
            <button className="px-5 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors">
              Sign up
            </button>
          </div>
        </div>
      </nav>

      <div className="max-w-[1600px] mx-auto px-6 py-6 flex gap-6">
        {/* Left Sidebar */}
        <aside className="w-64 shrink-0 space-y-4">
          {/* Categories */}
          <div className="bg-white rounded-lg border border-border p-4">
            <h3 className="mb-3 text-foreground">Categories</h3>
            <div className="space-y-1">
              {categories.map((category) => {
                const Icon = category.icon;
                return (
                  <button
                    key={category.id}
                    onClick={() => setSelectedCategory(category.id)}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                      selectedCategory === category.id
                        ? 'bg-primary/10 text-primary'
                        : 'hover:bg-accent text-foreground'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
                    <span className="text-sm">{category.name}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Join CTA */}
          <div className="bg-primary/5 rounded-lg border border-primary/20 p-4">
            <h4 className="text-foreground mb-2">Join our community</h4>
            <p className="text-sm text-muted-foreground mb-3">
              Start trading with thousands of users today
            </p>
            <button className="w-full px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors">
              Sign up free
            </button>
          </div>

          {/* How it works */}
          <div className="bg-white rounded-lg border border-border p-4">
            <h4 className="text-foreground mb-3">How it works</h4>
            <div className="space-y-3">
              <div className="flex gap-3">
                <div className="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm shrink-0">
                  1
                </div>
                <p className="text-sm text-muted-foreground">List items you want to trade</p>
              </div>
              <div className="flex gap-3">
                <div className="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm shrink-0">
                  2
                </div>
                <p className="text-sm text-muted-foreground">Browse and find items you need</p>
              </div>
              <div className="flex gap-3">
                <div className="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm shrink-0">
                  3
                </div>
                <p className="text-sm text-muted-foreground">Connect and exchange directly</p>
              </div>
            </div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="flex-1 min-w-0">
          {/* Category Shortcuts */}
          <div className="bg-white rounded-lg border border-border p-4 mb-6">
            <h3 className="text-foreground mb-4">Browse by category</h3>
            <div className="grid grid-cols-6 gap-4">
              {categories.slice(1, 7).map((category) => {
                const Icon = category.icon;
                return (
                  <button
                    key={category.id}
                    onClick={() => setSelectedCategory(category.id)}
                    className="flex flex-col items-center gap-2 p-3 rounded-lg hover:bg-accent transition-colors group"
                  >
                    <div className="w-12 h-12 rounded-full bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center transition-colors">
                      <Icon className="w-6 h-6 text-primary" />
                    </div>
                    <span className="text-xs text-center text-foreground">{category.name}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Featured Items */}
          <div className="bg-white rounded-lg border border-border p-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-foreground">Featured items</h3>
              <span className="text-sm text-muted-foreground">{items.length} items available</span>
            </div>

            {/* Item Grid */}
            <div className="grid grid-cols-5 gap-4 mb-6">
              {items.map((item) => (
                <div
                  key={item.id}
                  className="group bg-white border border-border rounded-lg overflow-hidden hover:shadow-md hover:border-primary/30 transition-all cursor-pointer"
                >
                  {/* Image */}
                  <div className="relative aspect-[4/3] bg-muted overflow-hidden">
                    <img
                      src={item.image}
                      alt={item.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleFavorite(item.id);
                      }}
                      className="absolute top-2 right-2 w-8 h-8 bg-white/90 hover:bg-white rounded-full flex items-center justify-center shadow-sm transition-colors"
                    >
                      <Heart
                        className={`w-4 h-4 ${
                          favorites.has(item.id)
                            ? 'fill-red-500 text-red-500'
                            : 'text-muted-foreground'
                        }`}
                      />
                    </button>
                  </div>

                  {/* Content */}
                  <div className="p-3 space-y-2">
                    <h4 className="text-sm text-foreground line-clamp-2 group-hover:text-primary transition-colors">
                      {item.title}
                    </h4>

                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center px-2 py-0.5 bg-green-50 text-green-700 text-xs rounded">
                        {item.status}
                      </span>
                      <span className="inline-flex items-center px-2 py-0.5 bg-primary/10 text-primary text-xs rounded">
                        {item.category}
                      </span>
                    </div>

                    <div className="flex items-center justify-between pt-1 border-t border-border">
                      <div className="flex items-center gap-1.5">
                        <User className="w-3.5 h-3.5 text-muted-foreground" />
                        <span className="text-xs text-muted-foreground">{item.owner}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-muted-foreground" />
                        <span className="text-xs text-muted-foreground">{item.time}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Load More Button */}
            <div className="flex justify-center pt-2">
              <button className="px-6 py-2.5 border border-border rounded-lg hover:border-primary hover:text-primary transition-colors">
                Load more items
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
