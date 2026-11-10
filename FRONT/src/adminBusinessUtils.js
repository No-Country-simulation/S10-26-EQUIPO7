export function filterAdminBusinesses(businesses, filters) {
  const query = filters.name.trim().toLocaleLowerCase();
  const city = filters.city.trim().toLocaleLowerCase();
  const category = filters.category.trim().toLocaleLowerCase();

  return businesses.filter((business) => (
    (!query || String(business.name || "").toLocaleLowerCase().includes(query)) &&
    (!city || String(business.city || "").toLocaleLowerCase().includes(city)) &&
    (!category || String(business.category || "").toLocaleLowerCase().includes(category))
  ));
}
