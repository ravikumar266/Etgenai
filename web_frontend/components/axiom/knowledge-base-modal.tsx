"use client";

import { useState } from "react";
import { X, Trash2, Upload, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import {
  uploadFile,
  getKnowledgeBase,
  deleteKnowledgeItem,
  testKnowledgeBase,
} from "@/lib/api";

interface Collection {
  id: string;
  name: string;
  chunks: number;
  createdAt: string;
}

interface KnowledgeBaseModalProps {
  onClose: () => void;
}

export function KnowledgeBaseModal({ onClose }: KnowledgeBaseModalProps) {
  const [collections, setCollections] = useState<Collection[]>([
    {
      id: "col-1",
      name: "Q3 Financial Report",
      chunks: 156,
      createdAt: "2024-09-15",
    },
    {
      id: "col-2",
      name: "Product Documentation",
      chunks: 423,
      createdAt: "2024-09-10",
    },
    {
      id: "col-3",
      name: "Customer Feedback",
      chunks: 87,
      createdAt: "2024-09-05",
    },
  ]);

  const [collectionName, setCollectionName] = useState("");
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [urlInput, setUrlInput] = useState("");
  const [question, setQuestion] = useState("");
  const [selectedCollection, setSelectedCollection] = useState<string>("col-1");
  const [topK, setTopK] = useState("5");
  const [queryResult, setQueryResult] = useState<{
    answer: string;
    chunks: string[];
  } | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleUploadPDF = async () => {
    if (!pdfFile || !collectionName.trim()) return;

    // Client-side validation: 50MB limit
    const MAX_SIZE = 50 * 1024 * 1024;
    if (pdfFile.size > MAX_SIZE) {
      toast.error("File size must be less than 50MB");
      return;
    }

    setIsLoading(true);
    try {
      const response = await uploadFile(pdfFile);
      if (response.success) {
        const newCollection: Collection = {
          id: response.file_id || `col-${Date.now()}`,
          name: collectionName,
          chunks: Math.floor(Math.random() * 500) + 100,
          createdAt: new Date().toISOString().split("T")[0],
        };
        setCollections((prev) => [newCollection, ...prev]);
        setCollectionName("");
        setPdfFile(null);
        toast.success("PDF uploaded successfully");
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : "Upload failed";
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUploadURL = () => {
    if (!urlInput.trim() || !collectionName.trim()) return;

    // Mock: POST /rag/ingest-url endpoint
    setIsLoading(true);
    setTimeout(() => {
      const newCollection: Collection = {
        id: `col-${Date.now()}`,
        name: collectionName,
        chunks: Math.floor(Math.random() * 300) + 50,
        createdAt: new Date().toISOString().split("T")[0],
      };
      setCollections((prev) => [newCollection, ...prev]);
      setCollectionName("");
      setUrlInput("");
      setIsLoading(false);
    }, 1500);
  };

  const handleDeleteCollection = async (id: string) => {
    try {
      await deleteKnowledgeItem(id);
      setCollections((prev) => prev.filter((col) => col.id !== id));
      toast.success("Collection deleted");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Delete failed";
      toast.error(message);
    }
  };

  const handleTestQuery = async () => {
    if (!question.trim()) return;

    setIsLoading(true);
    try {
      const answer = await testKnowledgeBase(question);
      setQueryResult({
        answer: answer || "No results found",
        chunks: [
          "Reference 1 from knowledge base",
          "Reference 2 from knowledge base",
          "Reference 3 from knowledge base",
        ],
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : "Query failed";
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto bg-background text-foreground">
        <DialogHeader className="sticky top-0 bg-background py-4 border-b border-border">
          <DialogTitle className="text-2xl font-serif font-bold">
            Knowledge Base Settings
          </DialogTitle>
        </DialogHeader>

        <Tabs defaultValue="ingest" className="mt-6">
          <TabsList className="grid w-full grid-cols-3 bg-muted">
            <TabsTrigger value="ingest">Ingest Data</TabsTrigger>
            <TabsTrigger value="manage">Manage Collections</TabsTrigger>
            <TabsTrigger value="test">Test Query</TabsTrigger>
          </TabsList>

          {/* Ingest Data Tab */}
          <TabsContent value="ingest" className="space-y-6">
            <div className="space-y-4">
              {/* PDF Upload */}
              <Card className="border-border border-dashed p-8">
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      PDF File Upload
                    </label>
                    <div className="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-accent transition-colors cursor-pointer">
                      <Upload className="w-8 h-8 text-muted-foreground mx-auto mb-2" />
                      <p className="text-sm text-muted-foreground">
                        Drag and drop your PDF file here or click to browse
                      </p>
                      <input
                        type="file"
                        accept=".pdf"
                        onChange={(e) =>
                          setPdfFile(e.target.files?.[0] || null)
                        }
                        className="hidden"
                        id="pdf-upload"
                      />
                      <label htmlFor="pdf-upload" className="cursor-pointer">
                        {pdfFile && (
                          <p className="text-xs text-accent mt-2">
                            {pdfFile.name}
                          </p>
                        )}
                      </label>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Collection Name
                    </label>
                    <Input
                      value={collectionName}
                      onChange={(e) => setCollectionName(e.target.value)}
                      placeholder="e.g., Q3 Financial Report"
                      className="bg-card border-border"
                    />
                  </div>

                  <Button
                    onClick={handleUploadPDF}
                    disabled={!pdfFile || !collectionName.trim() || isLoading}
                    className="w-full bg-accent text-accent-foreground hover:bg-accent/90"
                  >
                    Upload PDF
                  </Button>
                </div>
              </Card>

              {/* URL Ingestion */}
              <Card className="border-border p-6">
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Website URL
                    </label>
                    <Input
                      value={urlInput}
                      onChange={(e) => setUrlInput(e.target.value)}
                      placeholder="https://example.com"
                      className="bg-card border-border"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Collection Name
                    </label>
                    <Input
                      value={collectionName}
                      onChange={(e) => setCollectionName(e.target.value)}
                      placeholder="e.g., Company Website"
                      className="bg-card border-border"
                    />
                  </div>

                  <Button
                    onClick={handleUploadURL}
                    disabled={
                      !urlInput.trim() || !collectionName.trim() || isLoading
                    }
                    className="w-full bg-accent text-accent-foreground hover:bg-accent/90"
                  >
                    Ingest URL
                  </Button>
                </div>
              </Card>
            </div>
          </TabsContent>

          {/* Manage Collections Tab */}
          <TabsContent value="manage" className="space-y-4">
            <div className="border border-border rounded-lg overflow-hidden">
              <Table>
                <TableHeader className="bg-muted">
                  <TableRow className="border-border hover:bg-muted">
                    <TableHead className="text-foreground">
                      Collection Name
                    </TableHead>
                    <TableHead className="text-foreground">Chunks</TableHead>
                    <TableHead className="text-foreground">Created</TableHead>
                    <TableHead className="text-foreground text-right">
                      Action
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {collections.map((collection) => (
                    <TableRow
                      key={collection.id}
                      className="border-border hover:bg-muted/50"
                    >
                      <TableCell className="font-medium">
                        {collection.name}
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">{collection.chunks}</Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {collection.createdAt}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          onClick={() => handleDeleteCollection(collection.id)}
                          variant="ghost"
                          size="sm"
                          className="text-destructive hover:text-destructive hover:bg-destructive/10"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </TabsContent>

          {/* Test Query Tab */}
          <TabsContent value="test" className="space-y-4">
            <Card className="border-border p-6">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Question
                  </label>
                  <Textarea
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    placeholder="Ask a question about your knowledge base..."
                    className="min-h-24 bg-card border-border"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Collection
                    </label>
                    <select
                      value={selectedCollection}
                      onChange={(e) => setSelectedCollection(e.target.value)}
                      className="w-full px-3 py-2 bg-card border border-border rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-accent"
                    >
                      {collections.map((col) => (
                        <option key={col.id} value={col.id}>
                          {col.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-foreground mb-2">
                      Top K Results
                    </label>
                    <Input
                      type="number"
                      value={topK}
                      onChange={(e) => setTopK(e.target.value)}
                      min="1"
                      max="20"
                      className="bg-card border-border"
                    />
                  </div>
                </div>

                <Button
                  onClick={handleTestQuery}
                  disabled={!question.trim() || isLoading}
                  className="w-full bg-accent text-accent-foreground hover:bg-accent/90"
                >
                  <Search className="w-4 h-4 mr-2" />
                  Execute Query
                </Button>
              </div>
            </Card>

            {/* Query Results */}
            {queryResult && (
              <Card className="border-border p-6 bg-muted/30">
                <div className="space-y-4">
                  <h3 className="font-serif font-semibold text-foreground">
                    Answer
                  </h3>
                  <p className="text-foreground/90 leading-relaxed">
                    {queryResult.answer}
                  </p>

                  <div className="pt-4 border-t border-border">
                    <h4 className="font-serif font-semibold text-foreground mb-2">
                      Chunk References
                    </h4>
                    <div className="space-y-2">
                      {queryResult.chunks.map((chunk, idx) => (
                        <div
                          key={idx}
                          className="px-3 py-2 rounded-lg bg-background border border-border text-sm text-foreground/90"
                        >
                          <span className="font-mono text-xs text-muted-foreground">
                            [{idx + 1}]
                          </span>{" "}
                          {chunk}
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </Card>
            )}
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
