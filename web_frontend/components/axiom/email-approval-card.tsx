'use client'

import { Check, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface PendingEmail {
  id: string
  to: string
  subject: string
  body: string
}

interface EmailApprovalCardProps {
  email: PendingEmail
  onApprove: () => void
  onDeny: () => void
}

export function EmailApprovalCard({
  email,
  onApprove,
  onDeny,
}: EmailApprovalCardProps) {
  return (
    <Card className="bg-card border-border border-l-2 border-l-accent p-4 rounded-lg rounded-bl-none">
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Badge variant="outline" className="text-accent border-accent">
            Email Ready to Send
          </Badge>
        </div>

        <div className="space-y-2 bg-background/40 rounded p-3">
          <div>
            <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              To:
            </label>
            <p className="text-sm text-foreground mt-1">{email.to}</p>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              Subject:
            </label>
            <p className="text-sm text-foreground mt-1">{email.subject}</p>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              Body:
            </label>
            <p className="text-sm text-foreground/90 mt-1 whitespace-pre-wrap leading-relaxed">
              {email.body}
            </p>
          </div>
        </div>

        <div className="flex gap-2 pt-2">
          <Button
            onClick={onApprove}
            className="flex-1 bg-success text-success-foreground hover:bg-success/90"
            size="sm"
          >
            <Check className="w-4 h-4 mr-2" />
            Approve & Send
          </Button>
          <Button
            onClick={onDeny}
            variant="outline"
            className="flex-1 border-destructive text-destructive hover:bg-destructive/10"
            size="sm"
          >
            <X className="w-4 h-4 mr-2" />
            Deny
          </Button>
        </div>
      </div>
    </Card>
  )
}
