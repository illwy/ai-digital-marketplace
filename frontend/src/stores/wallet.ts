import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchWallet } from '../api/shop'
import type { WalletView } from '../types/api'

export const useWalletStore = defineStore('wallet', () => {
  const wallet = ref<WalletView | null>(null)
  let refreshSequence = 0

  async function refresh(): Promise<void> {
    const sequence = ++refreshSequence
    try {
      const { data } = await fetchWallet()
      if (sequence === refreshSequence) wallet.value = data.data
    } catch {
      if (sequence === refreshSequence) wallet.value = null
    }
  }

  function clear(): void {
    refreshSequence += 1
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
