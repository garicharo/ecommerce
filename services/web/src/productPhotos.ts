function lockFor(sku: string): number {
  let hash = 0;
  for (let i = 0; i < sku.length; i++) {
    hash = (hash * 31 + sku.charCodeAt(i)) >>> 0;
  }
  return hash;
}

function shot(id: string): string {
  return `https://images.unsplash.com/${id}?auto=format&fit=crop&w=600&h=600&q=80`;
}

const GOODS = [
  shot("photo-1441986300917-64674bd600d8"),
  shot("photo-1472851298512-301eb20ec205"),
  shot("photo-1526170375885-4d8ecf77b99f"),
] as const;

const STUDIO: Record<string, readonly string[]> = {
  footwear: [
    shot("photo-1542291026-7eec264c27ff"),
    shot("photo-1549298916-b41d501d3772"),
    shot("photo-1460353581641-37baddab0fa2"),
  ],
  electronics: [
    shot("photo-1505740420928-5e560c06d30e"),
    shot("photo-1593640408182-31c70c8268f5"),
    shot("photo-1583394838336-acd977736f90"),
    shot("photo-1517336714731-489689fd1ca8"),
  ],
  food: [
    shot("photo-1495474472287-4d71bcdd2085"),
    shot("photo-1511920170033-f8396924c348"),
    shot("photo-1452251889944-0c2ac337a2d6"),
  ],
  sports: [
    shot("photo-1517836357463-d25dfeac3438"),
    shot("photo-1571019614242-c5c01dee3ce1"),
    shot("photo-1518611012118-696072aa579a"),
  ],
  outdoors: [
    shot("photo-1504280390367-361c6d9f38f4"),
    shot("photo-1478131143081-80f7f84ca84d"),
    shot("photo-1523987355523-c7b5b0dd90a7"),
  ],
  kitchen: [
    shot("photo-1556911220-e15b29be8c8f"),
    shot("photo-1556909114-f6e7ad7d3136"),
    shot("photo-1585659722983-3a675dabf23d"),
  ],
  home: [
    shot("photo-1555041469-a586c61ea9bc"),
    shot("photo-1507473881161-95d2b854bb2c"),
    shot("photo-1493663284031-b7e3aefcae8e"),
  ],
  clothing: [
    shot("photo-1521572163474-6864f9cf17ab"),
    shot("photo-1434389677669-e08b4cac3105"),
    shot("photo-1523381210434-271e8be1f52b"),
  ],
  accessories: [
    shot("photo-1523275335684-37898b6baf30"),
    shot("photo-1572635196237-14b3f281195d"),
    shot("photo-1553062407-98eeb64c6a62"),
  ],
  beauty: [
    shot("photo-1596462502278-27bfdc403348"),
    shot("photo-1571781926291-c477ebfd024b"),
    shot("photo-1620916566398-39f1143ab7be"),
  ],
  games: [
    shot("photo-1610890716171-6b1bb98ffd09"),
    shot("photo-1632501641765-e568d28b0015"),
    shot("photo-1606166188520-2f7811958540"),
  ],
  books: [
    shot("photo-1544947950-fa07a98d237f"),
    shot("photo-1512820790803-83ca734da794"),
    shot("photo-1495446815901-a7297e633e8d"),
  ],
  stationery: [
    shot("photo-1455390582262-044cdead277a"),
    shot("photo-1513542789411-b6a5d4f31634"),
    shot("photo-1583484963886-cfe2bff2945f"),
  ],
  pets: [
    shot("photo-1583337130417-3346a1be7dee"),
    shot("photo-1601758228041-f3b2795255f1"),
  ],
  health: [
    shot("photo-1584308666744-24d5c474f2ae"),
    shot("photo-1471864190281-a93a3070b6de"),
  ],
  tools: [
    shot("photo-1504148453058-fb6657bbed4c"),
    shot("photo-1530124566582-a618bc2615dc"),
  ],
  goods: GOODS,
};

const KEYWORDS: Array<[string, string]> = [
  ["footwear", "shoe boot sneaker"],
  ["electronics", "mouse speaker headphone earbud charger cable keyboard laptop phone camera projector light usb"],
  ["food", "coffee tea protein mug flask thermos kettle"],
  ["sports", "yoga mat dumbbell jump rope skate band ball glove bike fitness"],
  ["outdoors", "tent hammock hose garden chair camp"],
  ["kitchen", "board kettle scale grinder spice rack"],
  ["home", "desk lamp pot candle blanket frame organizer clock art purifier"],
  ["clothing", "shirt"],
  ["accessories", "wallet sunglass backpack bag umbrella case"],
  ["beauty", "oil cream brush mask"],
  ["games", "game chess puzzle"],
  ["books", "book cookbook"],
  ["stationery", "pencil notebook paint tape"],
  ["pets", "dog leash"],
  ["health", "vitamin"],
  ["tools", "measure"],
];

function matchHay(hay: string): string | undefined {
  for (const [key, words] of KEYWORDS) {
    if (words.split(" ").some((word) => word && hay.includes(word))) {
      return key;
    }
  }
  return Object.keys(STUDIO).find((key) => hay.includes(key));
}

function bucket(category: string, name: string): string {
  const safeName = name.includes("<") ? "" : name;
  const fromName = matchHay(safeName.toLowerCase());
  if (fromName) {
    return fromName;
  }
  const fromCategory = matchHay(category.toLowerCase());
  if (fromCategory) {
    return fromCategory;
  }
  const hay = `${category} ${safeName}`.toLowerCase();
  if (hay.includes("home") || hay.includes("office")) {
    return "home";
  }
  if (hay.includes("food") || hay.includes("beverage")) {
    return "food";
  }
  return "goods";
}

export function relatedProductImage(name: string, category: string, sku: string): string {
  const photos = STUDIO[bucket(category, name)] ?? GOODS;
  return photos[lockFor(sku) % photos.length] ?? GOODS[0];
}

export function productImage(sku: string): string {
  return `https://picsum.photos/seed/${encodeURIComponent(sku)}/600/600`;
}

export function initials(name: string): string {
  return (name || "?").trim().slice(0, 2).toUpperCase();
}
