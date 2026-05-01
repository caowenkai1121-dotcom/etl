import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    currentPage: '',
    notifications: []
  }),
  actions: {
    toggleSidebar() { this.sidebarCollapsed = !this.sidebarCollapsed },
    setCurrentPage(page) { this.currentPage = page },
    addNotification(n) { this.notifications.unshift(n) }
  }
})
