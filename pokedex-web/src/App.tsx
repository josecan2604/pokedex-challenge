import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { BrowsePage } from './pages/BrowsePage'
import { CollectionPage } from './pages/CollectionPage'
import { LocalPokemonPage } from './pages/LocalPokemonPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PokemonDetailPage } from './pages/PokemonDetailPage'
import { RegisterPage } from './pages/RegisterPage'

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<BrowsePage />} />
        <Route path="pokemon/:idOrName" element={<PokemonDetailPage />} />
        <Route path="collection" element={<CollectionPage />} />
        <Route path="collection/:id" element={<LocalPokemonPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
