import { Navigate, Route, Routes } from "react-router-dom";
import { AdminLayout, RequireAuth, RequireCart } from "./gates";
import { AdminHome } from "./pages/AdminHome";
import { AdminImportJob } from "./pages/AdminImportJob";
import { AdminImports } from "./pages/AdminImports";
import { AdminOrders } from "./pages/AdminOrders";
import { AdminProductEdit } from "./pages/AdminProductEdit";
import { AdminProducts } from "./pages/AdminProducts";
import { CartPage } from "./pages/CartPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { LoginPage } from "./pages/LoginPage";
import { OrdersPage } from "./pages/OrdersPage";
import { ProductPage } from "./pages/ProductPage";
import { Shop } from "./pages/Shop";
import { SignupPage } from "./pages/SignupPage";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Shop />} />
      <Route path="/products/:sku" element={<ProductPage />} />
      <Route
        path="/cart"
        element={
          <RequireAuth>
            <RequireCart>
              <CartPage />
            </RequireCart>
          </RequireAuth>
        }
      />
      <Route
        path="/checkout"
        element={
          <RequireAuth>
            <RequireCart>
              <CheckoutPage />
            </RequireCart>
          </RequireAuth>
        }
      />
      <Route
        path="/orders"
        element={
          <RequireAuth>
            <OrdersPage />
          </RequireAuth>
        }
      />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<Navigate to="imports" replace />} />
        <Route path="products" element={<AdminProducts />} />
        <Route path="products/new" element={<AdminHome />} />
        <Route path="products/:sku" element={<AdminProductEdit />} />
        <Route path="imports" element={<AdminImports />} />
        <Route path="imports/:jobId" element={<AdminImportJob />} />
        <Route path="orders" element={<AdminOrders />} />
      </Route>
    </Routes>
  );
}
