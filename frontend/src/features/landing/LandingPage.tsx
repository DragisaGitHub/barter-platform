import { Link } from "react-router-dom";
import { ArrowRight, Repeat, Shield, Zap } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";

export function LandingPage() {
  return (
    <div className="min-h-screen">
      <nav className="border-b border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">
              Barter Platform
            </h1>
            <div className="flex items-center gap-3">
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Login
                </Button>
              </Link>
              <Link to="/register">
                <Button size="sm">Get Started</Button>
              </Link>
            </div>
          </div>
        </div>
      </nav>

      <main>
        <section className="py-20 lg:py-32 px-4 text-center">
          <div className="max-w-4xl mx-auto">
            <h2 className="text-4xl lg:text-6xl font-bold text-slate-900 dark:text-white mb-6">
              Trade What You Have for What You Need
            </h2>
            <p className="text-xl text-slate-600 dark:text-slate-400 mb-10 max-w-2xl mx-auto">
              Join our modern barter marketplace where you can exchange goods and services
              without cash. Build connections, save money, and trade sustainably.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link to="/register">
                <Button size="lg">
                  Start Trading
                  <ArrowRight className="size-5" />
                </Button>
              </Link>
              <Link to="/marketplace">
                <Button variant="outline" size="lg">
                  Browse Marketplace
                </Button>
              </Link>
            </div>
          </div>
        </section>

        <section className="py-16 px-4 bg-white dark:bg-slate-800">
          <div className="max-w-6xl mx-auto">
            <h3 className="text-3xl font-bold text-center text-slate-900 dark:text-white mb-12">
              Why Choose Barter Platform?
            </h3>
            <div className="grid md:grid-cols-3 gap-8">
              <Card>
                <CardContent className="pt-6">
                  <div className="bg-indigo-100 dark:bg-indigo-900/30 rounded-lg p-3 w-fit mb-4">
                    <Repeat className="size-8 text-indigo-600 dark:text-indigo-400" />
                  </div>
                  <h4 className="text-xl font-semibold text-slate-900 dark:text-white mb-2">
                    Easy Exchanges
                  </h4>
                  <p className="text-slate-600 dark:text-slate-400">
                    Intuitive platform makes it simple to list items, find matches, and
                    complete trades securely.
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="pt-6">
                  <div className="bg-emerald-100 dark:bg-emerald-900/30 rounded-lg p-3 w-fit mb-4">
                    <Shield className="size-8 text-emerald-600 dark:text-emerald-400" />
                  </div>
                  <h4 className="text-xl font-semibold text-slate-900 dark:text-white mb-2">
                    Safe & Secure
                  </h4>
                  <p className="text-slate-600 dark:text-slate-400">
                    Verified users, secure messaging, and community ratings ensure safe
                    transactions.
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardContent className="pt-6">
                  <div className="bg-violet-100 dark:bg-violet-900/30 rounded-lg p-3 w-fit mb-4">
                    <Zap className="size-8 text-violet-600 dark:text-violet-400" />
                  </div>
                  <h4 className="text-xl font-semibold text-slate-900 dark:text-white mb-2">
                    Fast Matching
                  </h4>
                  <p className="text-slate-600 dark:text-slate-400">
                    Smart algorithms connect you with relevant trades quickly, saving you
                    time and effort.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
