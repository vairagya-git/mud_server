import os

BASE = "/Users/I753307/Library/Mobile Documents/com~apple~CloudDocs/TechExamples/solr-jsonparsing/mud_server/src/main/resources/templates"

def page(title, content):
    return f'''<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{{fragments/layout :: layout(~{{::title}}, ~{{::#page-content}})}}">
<head><title>{title}</title></head>
<body>
<div id="page-content" th:fragment="content">
{content}
</div>
</body>
</html>
'''

files = {}

# ── day_stock_movement_key.html ───────────────────────────────────
# Controller puts: model.addAttribute("events", repo.findAll())
# Entity fields: id, code, description, date (LocalDate)
files["day_stock_movement/day_stock_movement_key.html"] = page("Day Stock Movement Keys — MudStock", """    <div class="page-header"><h1>Day Stock Movement Keys</h1></div>
    <div th:if="${csvMessage}" class="alert alert-success" th:text="${csvMessage}"></div>
    <div class="actions-bar" style="display:flex;gap:16px;flex-wrap:wrap">
        <form th:action="@{/day-stock-movement}" method="post" style="display:flex;gap:8px;align-items:flex-end">
            <div class="form-group" style="margin:0">
                <label>Code</label>
                <input type="text" name="code" placeholder="e.g. 2026-06-01" required />
            </div>
            <div class="form-group" style="margin:0">
                <label>Description</label>
                <input type="text" name="description" placeholder="optional" />
            </div>
            <div class="form-group" style="margin:0">
                <label>Date</label>
                <input type="date" name="event_date" />
            </div>
            <button type="submit" class="btn btn-primary">+ Add Key</button>
        </form>
    </div>
    <div class="card table-wrap" style="margin-top:20px">
        <div th:if="${events.isEmpty()}" class="table-empty">No keys yet.</div>
        <table th:unless="${events.isEmpty()}">
            <thead><tr><th>ID</th><th>Code</th><th>Description</th><th>Date</th></tr></thead>
            <tbody>
                <tr th:each="k : ${events}">
                    <td th:text="${k.id}"></td>
                    <td><code th:text="${k.code}"></code></td>
                    <td th:text="${k.description != null ? k.description : '&#8212;'}"></td>
                    <td th:text="${k.date != null ? k.date : '&#8212;'}"></td>
                </tr>
            </tbody>
        </table>
    </div>""")

# ── day_stock_movement_map.html ───────────────────────────────────
# Controller puts: mappings, stocks, days (DayStockMovementKey list)
# Entity DayStockMovementKey fields: id, code, description, date
files["day_stock_movement/day_stock_movement_map.html"] = page("DSM Mapping — MudStock", """    <div class="page-header">
        <h1>Day Stock Movement Mapping</h1>
        <div class="subtitle">Assign stocks to a movement key.</div>
    </div>
    <div th:if="${marketClosedWarning}" class="alert alert-error" th:text="${marketClosedWarning}"></div>
    <div class="card" style="max-width:540px">
        <form th:action="@{/day-stock-movement/mapping}" method="post">
            <div class="form-group">
                <label>Key (Code)</label>
                <select name="code" required>
                    <option value="">-- select --</option>
                    <option th:each="k : ${days}" th:value="${k.code}" th:text="${k.code} + ' (' + ${k.date != null ? k.date : 'no date'} + ')'"></option>
                </select>
            </div>
            <div class="form-group">
                <label>Stock Ticker</label>
                <input type="text" name="ticker" placeholder="AAPL" required />
            </div>
            <div class="actions-bar" style="margin-top:20px">
                <button type="submit" class="btn btn-primary">Save Mapping</button>
                <a href="/day-stock-movement" hx-get="/day-stock-movement" hx-target="#page-content" hx-swap="outerHTML" hx-push-url="true" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
    <div class="card table-wrap" style="margin-top:24px">
        <h3 style="margin-bottom:12px">Current Mappings</h3>
        <div th:if="${mappings.isEmpty()}" class="table-empty">No mappings yet.</div>
        <table th:unless="${mappings.isEmpty()}">
            <thead><tr><th>Day Code</th><th>Date</th><th>Ticker</th></tr></thead>
            <tbody>
                <tr th:each="m : ${mappings}">
                    <td><code th:text="${m.day_code}"></code></td>
                    <td th:text="${m.event_date != null ? m.event_date : '&#8212;'}"></td>
                    <td><span class="badge badge-grey" th:text="${m.ticker}"></span></td>
                </tr>
            </tbody>
        </table>
    </div>""")

# ── day_stock_movement_watchlist.html ─────────────────────────────
# Controller puts: watchlists, days (active keys), everydayWatchlistCode,
#                  marketClosedRemovedCount, marketClosedRemovedEntries
files["day_stock_movement/day_stock_movement_watchlist.html"] = page("DSM from Watchlist — MudStock", """    <div class="page-header">
        <h1>Populate DSM from Watchlist</h1>
        <div class="subtitle">Populate day stock movement mappings from an existing watchlist.</div>
    </div>
    <div th:if="${message}" class="alert alert-success" th:text="${message}"></div>
    <div th:if="${error}"   class="alert alert-error"   th:text="${error}"></div>
    <div th:if="${marketClosedRemovedCount != null}" class="alert alert-error">
        <strong th:text="${marketClosedRemovedCount}"></strong> key(s) hidden — market was closed:
        <ul style="margin:8px 0 0 16px">
            <li th:each="e : ${marketClosedRemovedEntries}" th:text="${e}"></li>
        </ul>
    </div>
    <div class="card" style="max-width:540px">
        <h3>Populate by Watchlist + Key</h3>
        <form th:action="@{/day-stock-movement/populate-watchlist}" method="post">
            <div class="form-group">
                <label>Day Key</label>
                <select name="dayEventMasterId" required>
                    <option value="">-- select --</option>
                    <option th:each="k : ${days}" th:value="${k.id}" th:text="${k.code} + ' (' + ${k.date != null ? k.date : 'no date'} + ')'"></option>
                </select>
            </div>
            <div class="form-group">
                <label>Watchlist</label>
                <select name="watchlistId" required>
                    <option value="">-- select --</option>
                    <option th:each="w : ${watchlists}" th:value="${w.id}" th:text="${w.name}"></option>
                </select>
            </div>
            <div class="actions-bar" style="margin-top:20px">
                <button type="submit" class="btn btn-primary">Populate</button>
                <a href="/day-stock-movement" hx-get="/day-stock-movement" hx-target="#page-content" hx-swap="outerHTML" hx-push-url="true" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>""")

# ── day_stock_movement_entries.html ───────────────────────────────
# Controller puts: entries (List<Map<String,Object>>)
# Map keys: ticker, day_code, master_event_date, cur_day_open, cur_day_close,
#           cur_day_high, cur_day_low, cur_day_volume, change_percent
files["day_stock_movement/day_stock_movement_entries.html"] = page("DSM Entries — MudStock", """    <div class="page-header">
        <h1>Day Stock Movement Entries</h1>
        <div class="subtitle">Total: <strong th:text="${entries.size()}">0</strong></div>
    </div>
    <div class="card table-wrap">
        <div th:if="${entries.isEmpty()}" class="table-empty">No entries found. Run the scheduler to populate.</div>
        <table th:unless="${entries.isEmpty()}">
            <thead>
                <tr>
                    <th>Ticker</th>
                    <th>Day Code</th>
                    <th>Date</th>
                    <th>Open</th>
                    <th>Close</th>
                    <th>High</th>
                    <th>Low</th>
                    <th>Volume</th>
                    <th>Change %</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="e : ${entries}">
                    <td><span class="badge badge-grey" th:text="${e.ticker}"></span></td>
                    <td><code th:text="${e.day_code}"></code></td>
                    <td th:text="${e.master_event_date}"></td>
                    <td th:text="${e.cur_day_open}"></td>
                    <td th:text="${e.cur_day_close}"></td>
                    <td th:text="${e.cur_day_high}"></td>
                    <td th:text="${e.cur_day_low}"></td>
                    <td th:text="${e.cur_day_volume}"></td>
                    <td th:text="${e.change_percent}"></td>
                </tr>
            </tbody>
        </table>
    </div>""")

# ── stock-watchlist/watchlist_detail.html ────────────────────────
# Controller puts: watchlist (entity with .stocks), allStocks
files["stock-watchlist/watchlist_detail.html"] = page("Watchlist Detail — MudStock", """    <div class="page-header">
        <h1 th:text="${watchlist.name}">Watchlist</h1>
        <div class="subtitle">Code: <code th:text="${watchlist.code}"></code></div>
    </div>
    <div class="card table-wrap">
        <div th:if="${watchlist.stocks.isEmpty()}" class="table-empty">No stocks in this watchlist yet.</div>
        <table th:unless="${watchlist.stocks.isEmpty()}">
            <thead><tr><th>#</th><th>Ticker</th><th>Name</th><th>Sector</th><th></th></tr></thead>
            <tbody>
                <tr th:each="s, stat : ${watchlist.stocks}">
                    <td th:text="${stat.count}"></td>
                    <td><span class="badge badge-grey" th:text="${s.ticker}"></span></td>
                    <td th:text="${s.name}"></td>
                    <td th:text="${s.sector != null ? s.sector : '&#8212;'}"></td>
                    <td>
                        <form th:action="@{/stock-watchlist/watchlists/{wid}/removeStock(wid=${watchlist.id})}" method="post" style="display:inline">
                            <input type="hidden" name="stockId" th:value="${s.id}" />
                            <button type="submit" class="btn btn-sm btn-danger">Remove</button>
                        </form>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    <div class="actions-bar" style="margin-top:24px">
        <a href="/stock-watchlist/watchlists" hx-get="/stock-watchlist/watchlists" hx-target="#page-content" hx-swap="outerHTML" hx-push-url="true" class="btn btn-secondary">&#8592; Watchlists</a>
    </div>""")

# ── stock-watchlist/watchlist_stock_overview.html ────────────────
# Controller puts: watchlists (List<Watchlist>)
# Each Watchlist has .stocks (Set<Stock>), .name, .code
files["stock-watchlist/watchlist_stock_overview.html"] = page("Watchlist Overview — MudStock", """    <div class="page-header">
        <h1>Watchlist Stock Overview</h1>
        <div class="subtitle">All watchlists with their associated stocks.</div>
    </div>
    <div th:each="w : ${watchlists}">
        <div class="card" style="margin-bottom:20px">
            <div style="display:flex;align-items:baseline;gap:12px;margin-bottom:12px">
                <h2 style="margin:0" th:text="${w.name}">Watchlist</h2>
                <span class="badge badge-grey" th:text="${w.code}"></span>
            </div>
            <div th:if="${w.stocks.isEmpty()}" class="table-empty">No stocks mapped.</div>
            <div th:unless="${w.stocks.isEmpty()}" class="tag-cloud">
                <span th:each="s : ${w.stocks}" class="badge badge-blue" th:text="${s.ticker}"></span>
            </div>
        </div>
    </div>
    <div th:if="${watchlists.isEmpty()}" class="card">
        <p class="table-empty">No watchlists found.</p>
    </div>""")

for rel, content in files.items():
    path = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(content)
    print(f"fixed: {rel}")

print("done fixes")
