-- Demo stock. Item 3 is deliberately out of stock so an order for it is REJECTED
-- (insufficient inventory), triggering the saga's compensating transaction.
INSERT INTO inventory (id, name, price, stock_amount)
VALUES (1, 'Mechanical Keyboard', 89.99, 25),
       (2, 'Wireless Mouse', 39.99, 100),
       (3, 'Limited Edition Headset', 199.99, 0)
ON CONFLICT (id) DO NOTHING;
