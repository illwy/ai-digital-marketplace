import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchWallet } from '../api/shop'
import type { WalletView } from '../types/api'

export const useWalletStore = defineStore('wallet', () => {
  const wallet = ref<WalletView | null>(null)

  async function refresh(): Promise<void> {
    try {
      const { data } = await fetchWallet()
      wallet.value = data.data
    } catch {
      wallet.value = null
    }
  }

  function clear(): void {
    wallet.value = null
  }

  function setBalance(balanceFen: number): void {
    if (wallet.value) {
      wallet.value = { ...wallet.value, balanceFen }
    } else {
      wallet.value = { userId: 0, balanceFen }
    }
  }

  return { wallet, refresh, clear, setBalance }
})
