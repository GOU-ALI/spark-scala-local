
import { NextResponse } from 'next/server';
import fs from 'fs';
import path from 'path';

const RESOURCES_DIR = path.resolve(process.cwd(), '../src/main/resources');

export async function GET() {
    try {
        if (!fs.existsSync(RESOURCES_DIR)) {
            return NextResponse.json({ error: 'Resources dir not found' }, { status: 404 });
        }

        const files = fs.readdirSync(RESOURCES_DIR)
            .filter(f => f.endsWith('.conf'));

        const jobs = files.map(file => {
            const content = fs.readFileSync(path.join(RESOURCES_DIR, file), 'utf-8');
            // Simple regex extraction for name (not perfect but fast)
            const nameMatch = content.match(/name\s*=\s*"([^"]+)"/);
            return {
                filename: file,
                name: nameMatch ? nameMatch[1] : file.replace('.conf', ''),
                content
            };
        });

        return NextResponse.json(jobs);
    } catch (error) {
        return NextResponse.json({ error: String(error) }, { status: 500 });
    }
}

export async function POST(request: Request) {
    try {
        const body = await request.json();
        const { filename, content } = body;

        if (!filename || !content) {
            return NextResponse.json({ error: 'Missing filename or content' }, { status: 400 });
        }

        fs.writeFileSync(path.join(RESOURCES_DIR, filename), content);
        return NextResponse.json({ success: true });
    } catch (error) {
        return NextResponse.json({ error: String(error) }, { status: 500 });
    }
}
