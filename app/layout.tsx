import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Commander Table",
  description: "Multiplayer Commander powered by Forge, with every battlefield in view.",
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en-US">
      <body className="antialiased">{children}</body>
    </html>
  );
}
