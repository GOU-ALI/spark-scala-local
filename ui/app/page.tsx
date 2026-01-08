
'use client';

import { useState, useEffect } from 'react';
import { Play, Save, FileText, Terminal, Activity, Database, Server } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Home() {
  const [jobs, setJobs] = useState<any[]>([]);
  const [selectedJob, setSelectedJob] = useState<any>(null);
  const [editorContent, setEditorContent] = useState('');
  const [logs, setLogs] = useState('');
  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    fetchJobs();
  }, []);

  const fetchJobs = async () => {
    const res = await fetch('/api/jobs');
    const data = await res.json();
    setJobs(data);
  };

  const handleSelectJob = (job: any) => {
    setSelectedJob(job);
    setEditorContent(job.content);
    setLogs('');
  };

  const handleSave = async () => {
    if (!selectedJob) return;
    await fetch('/api/jobs', {
      method: 'POST',
      body: JSON.stringify({ filename: selectedJob.filename, content: editorContent }),
    });
    fetchJobs(); // Refresh
  };

  const handleRun = async () => {
    if (!selectedJob) return;
    setIsRunning(true);
    setLogs('>>> Initializing Execution...\n');

    // Check if streaming
    const isStreaming = editorContent.includes('mode = "streaming"');
    if (isStreaming) {
      setLogs(l => l + '>>> Detected Streaming Job. Will run for preview (10s)...\n');
    }

    try {
      const res = await fetch('/api/run', {
        method: 'POST',
        body: JSON.stringify({ filename: selectedJob.filename, isStreaming }),
      });
      const result = await res.json();

      if (result.error) {
        setLogs(l => l + '\n>>> Execution Failed: ' + result.error);
        return;
      }

      setLogs(l => l + '>>> Execution Output:\n' + (result.stdout || '') + '\n' + (result.stderr || ''));
      setLogs(l => l + '\n>>> Finished.');
    } catch (e) {
      setLogs(l => l + '\n>>> Error: ' + String(e));
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <main className="min-h-screen bg-neutral-950 text-white font-sans selection:bg-purple-500/30">

      {/* Header */}
      <header className="border-b border-white/5 bg-black/20 backdrop-blur-md sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full overflow-hidden border-2 border-orange-500/50 shadow-lg shadow-orange-500/20">
              <img src="/lion-logo.png" alt="Lion Data Platform" className="w-full h-full object-cover" />
            </div>
            <h1 className="text-xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-orange-400 to-amber-200">
              Lion Data Platform
            </h1>
          </div>
          <div className="flex items-center gap-4 text-sm text-neutral-400">
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${isRunning ? 'bg-green-500 animate-pulse' : 'bg-neutral-600'}`}></span>
              {isRunning ? 'System Active' : 'System Idle'}
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto p-6 grid grid-cols-12 gap-6 h-[calc(100vh-4rem)]">

        {/* Sidebar - Job List */}
        <div className="col-span-3 h-full overflow-y-auto pr-2">
          <h2 className="text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-4">Pipeline Jobs</h2>
          <div className="space-y-2">
            {jobs.map((job) => (
              <motion.button
                key={job.filename}
                layoutId={job.filename}
                onClick={() => handleSelectJob(job)}
                className={`w-full text-left p-4 rounded-xl border transition-all duration-200 group relative overflow-hidden ${selectedJob?.filename === job.filename
                  ? 'border-purple-500/50 bg-purple-500/10 shadow-[0_0_20px_-5px_rgba(168,85,247,0.3)]'
                  : 'border-white/5 bg-white/5 hover:bg-white/10 hover:border-white/10'
                  }`}
              >
                <div className="relative z-10 flex items-center gap-3">
                  <div className={`p-2 rounded-lg ${selectedJob?.filename === job.filename ? 'bg-purple-500/20 text-purple-300' : 'bg-white/5 text-neutral-400'}`}>
                    {job.content.includes('spark') ? <Activity size={16} /> : <Database size={16} />}
                  </div>
                  <div>
                    <h3 className={`font-medium ${selectedJob?.filename === job.filename ? 'text-white' : 'text-neutral-300'}`}>
                      {job.name}
                    </h3>
                    <p className="text-xs text-neutral-500 mt-1 font-mono truncate">{job.filename}</p>
                  </div>
                </div>
              </motion.button>
            ))}

            {jobs.length === 0 && (
              <div className="text-neutral-500 text-sm italic p-4">No jobs found.</div>
            )}
          </div>
        </div>

        {/* Main Content - Editor & Logs */}
        <div className="col-span-9 flex flex-col gap-6 h-full overflow-hidden pb-6">

          {selectedJob ? (
            <>
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex-1 border border-white/5 bg-neutral-900/50 rounded-2xl overflow-hidden flex flex-col shadow-2xl"
              >
                {/* Toolbar */}
                <div className="h-12 border-b border-white/5 bg-white/5 flex items-center justify-between px-4">
                  <div className="flex items-center gap-2 text-sm text-neutral-400">
                    <FileText size={14} />
                    <span>{selectedJob.filename}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleSave}
                      className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-neutral-300 text-sm transition-colors"
                    >
                      <Save size={14} /> Save
                    </button>
                    <button
                      onClick={handleRun}
                      disabled={isRunning}
                      className={`flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all shadow-lg
                           ${isRunning
                          ? 'bg-neutral-800 text-neutral-500 cursor-not-allowed'
                          : 'bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-500 hover:to-blue-500 text-white shadow-purple-500/20'
                        }`}
                    >
                      {isRunning ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : <Play size={14} />}
                      {isRunning ? 'Running...' : 'Run Pipeline'}
                    </button>
                  </div>
                </div>

                {/* Editor Area */}
                <div className="flex-1 relative">
                  <textarea
                    value={editorContent}
                    onChange={(e) => setEditorContent(e.target.value)}
                    className="w-full h-full bg-transparent p-4 font-mono text-sm text-neutral-200 focus:outline-none resize-none"
                    spellCheck={false}
                  />
                </div>
              </motion.div>

              {/* Logs Console */}
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="h-64 border border-white/5 bg-black rounded-2xl overflow-hidden flex flex-col"
              >
                <div className="h-10 border-b border-white/10 bg-white/5 flex items-center px-4 gap-2">
                  <Terminal size={14} className="text-neutral-500" />
                  <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">Execution Logs</span>
                </div>
                <div className="flex-1 p-4 overflow-y-auto">
                  <div className="font-mono text-xs whitespace-pre-wrap flex flex-col gap-1">
                    {(logs || 'Ready to execute...').split('\n').map((line, i) => {
                      let color = 'text-neutral-400';
                      const lower = line.toLowerCase();

                      if (lower.includes('warning')) {
                        color = 'text-yellow-400 font-bold';
                      } else if (lower.includes('error') || lower.includes('exception') || lower.includes('failed')) {
                        color = 'text-red-400 font-bold';
                      } else if (lower.includes('completed') || lower.includes('finished') || lower.includes('success')) {
                        color = 'text-green-400 font-bold';
                      }

                      return (
                        <div key={i} className={color}>
                          {line}
                        </div>
                      );
                    })}
                  </div>
                </div>
              </motion.div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-neutral-500">
              <div className="w-20 h-20 rounded-2xl bg-white/5 flex items-center justify-center mb-4">
                <Server size={32} />
              </div>
              <p className="text-lg font-medium text-neutral-400">Select a Job to Configure</p>
              <p className="text-sm">Choose a pipeline from the sidebar to start.</p>
            </div>
          )}

        </div>
      </div>
    </main>
  );
}
