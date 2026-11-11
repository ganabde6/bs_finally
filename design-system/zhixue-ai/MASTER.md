# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** zhixue-ai
**Updated:** 2026-11-11(全站换色:小米橙 / 墨黑中性底 / 零绿)
**Category:** AI/Chatbot Platform

---

## Global Rules

### Color Palette

| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary | `#FF6900` | `--color-primary` |
| On Primary | `#FFFFFF` | `--color-on-primary` |
| Secondary | `#FF964D` | `--color-secondary` |
| Accent/CTA | `#FFB480` | `--color-accent` |
| Background | `#F7F8FA` | `--color-background` |
| Foreground | `#1F2329` | `--color-foreground` |
| Muted | `#E8ECF1` | `--color-muted` |
| Border | `#E8ECF1` | `--color-border` |
| Destructive | `#DC2626` | `--color-destructive` |
| Ring | `#FF6900` | `--color-ring` |
| Success(答对/成功) | `#409EFF` | `--el-color-success` |

**Color Notes:** 小米橙品牌色(与登录页一致)+ 墨黑中性底;全站禁止绿色(主题与功能色均不含绿),成功/答对统一用蓝色。
Element Plus 主色衍生:light-3 `#FF964D` / light-5 `#FFB480` / light-7 `#FFD2B3` / light-8 `#FFE1CC` / light-9 `#FFF0E6` / dark-2 `#CC5400`。
ECharts 品牌色板(多序列图表,无绿):`#FF6900, #409EFF, #9B59F5, #F7BA1E, #F56C6C, #22B8CF`。

### Typography

- **Font stack:** `Poppins, -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif`(见 `style.css` body;不加载外部字体,离线可用)
- **Mood:** warm orange, clean, professional, friendly education

### Spacing Variables

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Shadow Depths

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | Modals, dropdowns |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | Hero images, featured cards |

---

## Component Specs

### Buttons

```css
/* Primary Button */
.btn-primary {
  background: #FF6900;
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: transparent;
  color: #FF6900;
  border: 2px solid #FF6900;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}
```

### Cards

```css
.card {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-md);
  transition: all 200ms ease;
  cursor: pointer;
}

.card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}
```

### Inputs

```css
.input {
  padding: 12px 16px;
  border: 1px solid #E4E7ED;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 200ms ease;
}

.input:focus {
  border-color: #FF6900;
  outline: none;
  box-shadow: 0 0 0 3px #FF690020;
}
```

### Modals

```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-xl);
  max-width: 500px;
  width: 90%;
}
```

### Sidebar / Header(BaseLayout)

```css
.aside {
  background: linear-gradient(180deg, #1F2329 0%, #2A2F3A 100%);
}
/* 菜单激活态:rgba(255, 150, 77, 0.22) 底 + #FF6900 左侧指示条 */
/* 顶栏底边线 #E8ECF1;主区背景 #F7F8FA */
```

### Stat Card(全局 .stat-card)

- 背景:白色 → `#FAFBFC` 135° 渐变
- 顶条:4px,`linear-gradient(90deg, #FF6900, #FF964D)`
- 右下装饰光斑:`rgba(255, 150, 77, 0.14)` 径向渐变
- 悬停:`translateY(-4px)` + `0 12px 24px rgba(255, 105, 0, 0.16)`
- 数字:`font-variant-numeric: tabular-nums`

### Style Guidelines

**Style:** AI-Native UI

**Keywords:** Chatbot, conversational, voice, assistant, agentic, ambient, minimal chrome, streaming text, AI interactions

**Best For:** AI products, chatbots, voice assistants, copilots, AI-powered tools, conversational interfaces

**Key Effects:** Typing indicators (3-dot pulse), streaming text animations, pulse animations, context cards, smooth reveals

### Page Pattern

**Pattern Name:** AI Personalization Landing

- **Conversion Strategy:** 20%+ conversion with personalization. Requires analytics integration. Fallback for new users.
- **CTA Placement:** Context-aware placement based on user segment
- **Section Order:** 1. Dynamic hero (personalized), 2. Relevant features, 3. Tailored testimonials, 4. Smart CTA

---

## Anti-Patterns (Do NOT Use)

- ❌ Heavy chrome
- ❌ Slow response feedback
- ❌ 任何绿色(主题色、功能色、图表色一律不含绿;答对/成功 = 蓝 `#409EFF`)

### Additional Forbidden Patterns

- ❌ **Emojis as icons** — Use SVG icons (Heroicons, Lucide, Simple Icons)
- ❌ **Missing cursor:pointer** — All clickable elements must have cursor:pointer
- ❌ **Layout-shifting hovers** — Avoid scale transforms that shift layout
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio
- ❌ **Instant state changes** — Always use transitions (150-300ms)
- ❌ **Invisible focus states** — Focus states must be visible for a11y

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] No emojis used as icons (use SVG instead)
- [ ] All icons from consistent icon set (Heroicons/Lucide)
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Light mode: text contrast 4.5:1 minimum
- [ ] Focus states visible for keyboard navigation
- [ ] `prefers-reduced-motion` respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px
- [ ] No content hidden behind fixed navbars
- [ ] No horizontal scroll on mobile
- [ ] 全站零绿:grep 校验无青绿系 hex(#0D9488/#2DD4BF/#0F766E/#0B3B37/#134E4A/#67C23A/#F0F9EB/#E8F1F4 等)
