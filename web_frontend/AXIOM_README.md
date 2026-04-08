# Axiom - Professional AI Agent Interface

A production-ready AI agent application with real backend integration, warm professional design, markdown rendering, and enterprise features.

## Overview

Axiom is a sophisticated web interface for interacting with an autonomous AI agent. It features:

- **Warm Professional Design** - Terracotta and charcoal color palette
- **Real Backend Integration** - Connects to `https://etgenainew.onrender.com`
- **Markdown Rendering** - AI responses with syntax-highlighted code blocks
- **Knowledge Base Management** - PDF uploads with 50MB validation
- **Email Approval System** - Review and approve automated emails
- **Theme System** - Light, dark, and system modes

## Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Opens http://localhost:3000
```

## Key Features

### 1. Warm Professional Design
- Light mode: Clean white with charcoal text and warm terracotta accents
- Dark mode: Deep charcoal with off-white text and vibrant terracotta accents
- System mode: Follows OS preference automatically

### 2. Markdown Message Rendering
AI responses support full GitHub Flavored Markdown:
- Syntax-highlighted code blocks
- Tables and lists
- Blockquotes and links
- Proper dark mode styling

### 3. Real Backend Integration
All 7+ API endpoints connected and working:
- Chat messaging with threads
- File uploads (PDF, 50MB limit)
- Knowledge base management
- Email approval workflow

### 4. File Upload Validation
- Strict 50MB client-side limit
- Instant error feedback via toast
- Prevents backend overload

### 5. Professional UI
- Clean sidebar header (removed icon/subtitle)
- Theme toggle in footer
- Responsive design
- Accessible components

## File Structure

```
app/
├── globals.css              ← Warm color palette
├── layout.tsx               ← Root layout with theme provider
└── page.tsx                 ← Main app component

components/axiom/
├── sidebar.tsx              ← Chat history & settings
├── chat-workspace.tsx       ← Chat interface with markdown
├── knowledge-base-modal.tsx ← PDF upload & KB management
├── email-approval-card.tsx  ← Email review card
├── markdown-renderer.tsx    ← Markdown to HTML renderer
└── theme-toggle.tsx         ← Theme switcher

lib/
└── api.ts                   ← Complete API service (220 lines)

Documentation/
├── AXIOM_README.md          ← This file
├── QUICK_START.md           ← Quick reference guide
├── PROFESSIONAL_SETUP.md    ← Detailed setup guide
├── BUILD_SUMMARY.md         ← What was built
├── FEATURES_GUIDE.md        ← Features with examples
└── VERIFICATION_CHECKLIST.md ← Build verification
```

## Color Palette

### Light Mode
```
Background:  oklch(0.98 0 0)    - Clean white
Primary:     oklch(0.18 0 0)    - Rich charcoal
Accent:      oklch(0.62 0.18 35) - Warm terracotta
Borders:     oklch(0.88 0 0)    - Soft gray
```

### Dark Mode
```
Background:  oklch(0.09 0 0)    - Deep charcoal
Primary:     oklch(0.93 0 0)    - Off-white
Accent:      oklch(0.65 0.18 35) - Vibrant terracotta
Borders:     oklch(0.16 0 0)    - Subtle dark
```

Both modes use hue 35° for consistent warm aesthetic.

## API Integration

### Base URL
```
https://etgenainew.onrender.com
```

### Endpoints Implemented

**Chat**
- `POST /chat` - Send message and get response
- `GET /threads` - List all threads
- `GET /threads/{id}` - Get thread messages

**Files**
- `POST /upload-pdf` - Upload PDF (50MB max)

**Knowledge Base**
- `GET /knowledge-base` - List items
- `DELETE /knowledge-base/{id}` - Remove item
- `POST /knowledge-base/test` - Test RAG query

**Email**
- `POST /email-approvals/{id}/approve` - Approve email
- `POST /email-approvals/{id}/reject` - Reject email

See `lib/api.ts` for implementation details.

## Component Hierarchy

```
Home (page.tsx)
├── Sidebar
│   ├── Chat History
│   ├── New Chat Button
│   ├── Theme Toggle
│   └── System Status
├── ChatWorkspace
│   ├── Messages (with MarkdownRenderer)
│   ├── EmailApprovalCard
│   └── Input Area
└── KnowledgeBaseModal
    ├── Ingest Data Tab
    ├── Manage Collections Tab
    └── Test Query Tab
```

## Markdown Features

### Code Blocks
```python
def analyze_data():
    return calculate_metrics()
```

Language detection and syntax highlighting for:
- Python, JavaScript, TypeScript
- SQL, HTML, CSS
- JSON, YAML, Bash
- And 50+ more languages

### Other Markdown
- **Bold** and *italic* text
- [Links](https://example.com)
- > Blockquotes
- Lists (ordered and unordered)
- Tables with borders
- Headings (h1 through h6)

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Enter` | Send message |
| `Tab` | Navigate elements |
| `Escape` | Close modals |

## Error Handling

All operations include proper error handling:
- Try-catch blocks on API calls
- User-friendly error messages
- Toast notifications (Sonner)
- Graceful fallbacks

Example error scenarios:
- File too large → "File size must be less than 50MB"
- API timeout → Shows error with retry option
- Invalid input → Prevents submission

## Accessibility

WCAG AA+ compliant:
- Semantic HTML structure
- ARIA labels and roles
- Keyboard navigation support
- Color contrast ratios
- Focus indicators

## Performance

- Code splitting with dynamic imports
- Optimized re-renders
- Efficient markdown rendering
- Proper caching headers
- Lazy loading where appropriate


### Environment Variables
None required - uses hardcoded backend URL.

For custom setup:
```env
NEXT_PUBLIC_API_BASE_URL=https://etgenainew.onrender.com
```

## Development

### Local Development
```bash
npm run dev
```

### Build for Production
```bash
npm run build
npm start
```

## Troubleshooting

### Backend not responding
- Check backend status: https://etgenainew.onrender.com
- Verify network connection
- Check browser console for errors

### File upload fails
- Ensure file is under 50MB
- Check file format (.pdf)
- Try smaller test file

### Markdown not rendering
- Ensure AI response contains markdown
- Check message in browser console
- Refresh page if needed

### Theme not changing
- Check browser settings
- Ensure cookies enabled
- Try system mode

## Support & Contributing

- **Issue Reports**: Create GitHub issue with details
- **Feature Requests**: Describe use case clearly
- **Pull Requests**: Follow existing code style

## Technologies Used

- **Framework**: Next.js 16
- **UI Library**: React 19
- **Styling**: Tailwind CSS v4
- **Components**: shadcn/ui
- **Theme**: next-themes
- **Markdown**: react-markdown + remark-gfm
- **Notifications**: Sonner
- **Icons**: Lucide React
- **API**: Fetch API

## What's Included

✅ Production-ready code
✅ Real backend integration (no mocks)
✅ Full TypeScript support
✅ Professional design system
✅ Markdown rendering
✅ File validation
✅ Error handling
✅ Theme system
✅ Responsive design
✅ Comprehensive documentation

## Next Steps

1. **Start Development**
   ```bash
   npm install && npm run dev
   ```

2. **Test Features**
   - Send chat messages
   - Upload files
   - Test knowledge base
   - Try different themes

3. **Customize**
   - Adjust colors in `app/globals.css`
   - Add new API endpoints in `lib/api.ts`
   - Extend components as needed

---

**Made with ❤️ for AI-powered applications**

