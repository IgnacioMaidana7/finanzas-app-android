export const MAX_TRANSACTIONS = 1000;
const LONG_MAX = 9223372036854775807n;

export class ApiError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

// Explicit UTC contract: [first day inclusive, next month exclusive).
export function monthRange(month) {
  if (!/^(20\d{2})-(0[1-9]|1[0-2])$/.test(month ?? '')) {
    throw new ApiError(400, 'INVALID_MONTH');
  }
  const [year, number] = month.split('-').map(Number);
  return { month, start: Date.UTC(year, number - 1, 1), end: Date.UTC(year, number, 1) };
}

export function summarize(rows, range) {
  if (rows.length > MAX_TRANSACTIONS) throw new ApiError(422, 'SUMMARY_TOO_LARGE');
  let income = 0n;
  let expense = 0n;
  for (const row of rows) {
    const amount = typeof row.amount === 'bigint' ? row.amount :
      Number.isSafeInteger(row.amount) ? BigInt(row.amount) : null;
    const date = typeof row.date === 'bigint' ? Number(row.date) : row.date;
    if (amount === null || amount < 0n || amount > LONG_MAX ||
        !['INGRESO', 'GASTO'].includes(row.type) || !Number.isSafeInteger(date) ||
        date < range.start || date >= range.end) {
      throw new ApiError(422, 'INVALID_TRANSACTION_DATA');
    }
    if (row.type === 'INGRESO') income += amount;
    else expense += amount;
  }
  // The Android client uses Long. Reject overflow instead of rounding/saturating.
  if (income > LONG_MAX || expense > LONG_MAX) throw new ApiError(422, 'AMOUNT_OVERFLOW');
  return {
    month: range.month, timeZone: 'UTC', unit: 'ARS_INTEGER',
    income: income.toString(), expense: expense.toString(),
    net: (income - expense).toString(), transactionCount: rows.length
  };
}
