"use client";
import Link from"next/link";import{usePathname,useRouter}from"next/navigation";import{useState}from"react";import{createClient}from"@/lib/supabase/client";import type{Access}from"@/lib/auth";
type Item={href:string;label:string;icon:string;show:(a:Access)=>boolean};
const nav:Item[]=[
{href:"/dashboard",label:"Overview",icon:"◫",show:()=>true},
{href:"/dashboard/orders",label:"Orders",icon:"▤",show:a=>a.isAdmin||a.roles.includes("orders")||a.roles.includes("finance")},
{href:"/dashboard/sms",label:"SMS & activation",icon:"✉",show:a=>a.isAdmin||a.roles.includes("orders")},
{href:"/dashboard/purchase-requests",label:"Purchase requests",icon:"▧",show:a=>a.isAdmin||a.roles.includes("finance")},
{href:"/dashboard/wallet",label:"Wallets & deposits",icon:"＄",show:a=>a.isAdmin||a.roles.includes("finance")},
{href:"/dashboard/services",label:"Services & pricing",icon:"◇",show:a=>a.isAdmin},
{href:"/dashboard/customers",label:"Customers",icon:"♧",show:a=>a.isAdmin},
{href:"/dashboard/subscriptions",label:"Subscriptions",icon:"▣",show:a=>a.isAdmin},
{href:"/dashboard/team",label:"Team & access",icon:"♙",show:a=>a.isAdmin},
{href:"/dashboard/settings",label:"Payment settings",icon:"⚙",show:a=>a.isAdmin||a.roles.includes("finance")}
];
export function DashboardShell({access,children}:{access:Access;children:React.ReactNode}){const path=usePathname(),router=useRouter();const[open,setOpen]=useState(false);async function logout(){const sb=createClient();await sb.auth.signOut();router.replace("/login");router.refresh();}const title=path.split("/").pop()?.replaceAll("-"," ")||"Overview";return <div className="shell"><aside className={"sidebar "+(open?"open":"")}><div className="brand"><div className="brand-icon">N1</div><div><b>Number One</b><small>CONTROL CENTER</small></div></div><div className="nav-caption">WORKSPACE</div><nav className="nav">{nav.filter(item=>item.show(access)).map(item=><Link key={item.href} href={item.href} className={path===item.href?"active":""} onClick={()=>setOpen(false)}><span>{item.icon}</span>{item.label}</Link>)}</nav><div className="side-bottom"><div className="muted" style={{color:"#8194ad",padding:"10px 12px"}}>● Role-protected access</div><button className="logout" onClick={logout}>↪ Sign out</button></div></aside><main className="main"><header className="topbar"><button className="mobile-menu" onClick={()=>setOpen(true)}>☰</button><div><div className="crumb">NUMBER ONE / OPERATIONS</div><strong>{title}</strong></div><div className="user"><div className="avatar">{access.email.slice(0,1).toUpperCase()}</div><div><b>{access.email}</b><div className="muted">{access.isAdmin?"Administrator":access.roles.join(" · ")+" staff"}</div></div></div></header><div className="content">{children}</div></main>{open&&<button className="scrim show" onClick={()=>setOpen(false)} aria-label="Close menu"/>}</div>;}