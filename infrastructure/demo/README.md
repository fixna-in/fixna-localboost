# Shared demo infrastructure (fixna.in)

Templates for the free-tier demo stack (Neon + **Render** + Vercel).
See [docs/07-operations/deployment.md](../../docs/07-operations/deployment.md).

| File | Use |
|------|-----|
| `render.env.example` | Render dashboard environment variables |
| `vercel.env.example` | Vercel project environment variables |
| `fly.secrets.example.env` | Unused — Fly requires payment card |

Never commit files containing real passwords or JWT secrets.
