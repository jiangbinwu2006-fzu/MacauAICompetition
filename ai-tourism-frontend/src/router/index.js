import { createRouter, createWebHistory } from 'vue-router'
import { ensureGuestSession } from '../utils/guestSession.js'

const Home = () => import('../views/Home.vue')
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const Explore = () => import('../views/Explore.vue')
const Ops = () => import('../views/Ops.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/explore' },
    { path: '/home', name: 'Home', component: Home, meta: { requiresAuth: true } },
    { path: '/login', name: 'Login', component: Login },
    { path: '/register', name: 'Register', component: Register },
    { path: '/explore', name: 'Explore', component: Explore, meta: { requiresSession: true } },
    { path: '/ops', name: 'Ops', component: Ops }
  ]
})

// 临时关闭鉴权放行开关，后端就绪后改为 false 恢复校验
const AUTH_DISABLED = false

router.beforeEach(async (to) => {
  if (AUTH_DISABLED) {
    return true
  }
  const isAuthenticated = !!localStorage.getItem('token')
  if (to.meta.requiresAuth && !isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresSession && !isAuthenticated) {
    try {
      await ensureGuestSession()
    } catch (error) {
      return { path: '/login', query: { redirect: to.fullPath, guest_error: '1' } }
    }
  }
  if ((to.path === '/login' || to.path === '/register') && isAuthenticated) {
    return { path: '/home' }
  }
  return true
})

export default router


