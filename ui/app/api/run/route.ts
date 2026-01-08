
import { NextResponse } from 'next/server';
import { exec } from 'child_process';
import path from 'path';

const PROJECT_ROOT = path.resolve(process.cwd(), '../');

export async function POST(request: Request) {
    try {
        const body = await request.json();
        const { filename, isStreaming } = body;

        if (!filename) {
            return NextResponse.json({ error: 'Missing filename' }, { status: 400 });
        }

        if (filename.includes('docker')) {
            return NextResponse.json({
                error: 'This configuration is for Docker execution only (uses /app/ paths). Please select "BatchJobExample" to run locally, or use "docker-compose up" in the terminal.'
            }, { status: 400 });
        }

        // Command construction
        // Assumes JAVA_HOME etc are set in the env or we fallback to system java
        // Ideally we should use the absolute path to java if we verified it earlier
        const javaCmd = 'java';
        const jarPath = 'target/spark-scala-local-1.0-SNAPSHOT.jar';
        const configPath = `src/main/resources/${filename}`;

        // Add opens for Java 17+
        const opens = '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED';

        const command = `${javaCmd} ${opens} -jar ${jarPath} ${configPath}`;

        // Note: For streaming jobs, this will block indefinitely if we await it.
        // For MVP, we will execute with a timeout or just detached if streaming.
        // However, exec() buffers output. spawn() is better for streaming logs.
        // For simplicity of this MVP API: we will just run it and return output (Batch) 
        // or run for a short time (Streaming) if we want to preview.

        // Strategy: Just run exec. If it's streaming, the user will likely see it timeout or hanging.
        // Better: let's run it with a timeout if it's explicitly marked streaming?
        // Or just run it.

        const timeout = isStreaming ? 10000 : 60000; // 10s for streaming preview, 60s for batch

        return new Promise((resolve) => {
            exec(command, { cwd: PROJECT_ROOT, timeout, maxBuffer: 1024 * 1024 * 5 }, (error, stdout, stderr) => {
                // If error is timeout (code 143 or signal SIGTERM), it might be acceptable for streaming
                resolve(NextResponse.json({
                    success: !error || (error as any).signal === 'SIGTERM',
                    stdout,
                    stderr: error ? error.message + '\n' + stderr : stderr
                }));
            });
        });

    } catch (error) {
        return NextResponse.json({ error: String(error) }, { status: 500 });
    }
}
