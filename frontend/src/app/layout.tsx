import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { Header } from "@/components/Header";
import "./globals.css";

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin", "latin-ext"] });
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] });

export const metadata: Metadata = {
  title: { default: "Saga Pattern Demo", template: "%s · Saga Pattern Demo" },
  description: "Demo Saga pattern (orchestration): đặt món ăn online qua bốn microservice",
};

export const viewport: Viewport = { colorScheme: "dark", themeColor: "#020617" };

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="vi" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded focus:bg-sky-600 focus:px-3 focus:py-2 focus:text-white"
        >
          Bỏ qua, tới nội dung
        </a>
        <Header />
        <main id="main" className="flex-1 pb-10">
          {children}
        </main>
      </body>
    </html>
  );
}
