import type { Metadata } from "next";
import { Poppins, Roboto } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { ThemeProvider } from "@/components/providers/theme-provider";
import QueryProvider from "@/components/providers/query-provider";
import { ClientOnly } from "@/components/providers/client-only";
import { BRAND } from "@/lib/brand";

const poppinsHeading = Poppins({
  subsets: ["latin"],
  variable: "--font-heading",
  weight: ["400", "500", "600", "700"],
});

const robotoSans = Roboto({
  subsets: ["latin"],
  variable: "--font-geist-sans",
  weight: ["400", "500", "700"],
});

export const metadata: Metadata = {
  title: BRAND.name,
  description: BRAND.description,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={cn(
        "h-full",
        "antialiased",
        robotoSans.variable,
        poppinsHeading.variable,
      )}
    >
      <body className="min-h-full flex flex-col">
        <ClientOnly>
          <QueryProvider>
            <ThemeProvider
            >
              {children}
            </ThemeProvider>
          </QueryProvider>
        </ClientOnly>
      </body>
    </html>
  );
}
