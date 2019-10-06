insert into product (code, name, workspace_id, company_id) values ('P01', 'Samsung AX12', 1, 1);

insert into link (id, url, sku, brand, price, status, product_id, workspace_id, company_id) values

-- for PAUSED status

-- SUITABLE statuses
    ( 1,'www.inprice.io/001', '001', 'inprice', 1.00, 'RENEWED', 1, 1, 1),
    ( 2,'www.inprice.io/002', '002', 'inprice', 1.00, 'BE_IMPLEMENTED', 1, 1, 1),
    ( 3,'www.inprice.io/003', '003', 'inprice', 1.00, 'IMPLEMENTED', 1, 1, 1),
    ( 4,'www.inprice.io/004', '004', 'inprice', 1.00, 'NOT_AVAILABLE', 1, 1, 1),
    ( 5,'www.inprice.io/005', '005', 'inprice', 1.00, 'READ_ERROR', 1, 1, 1),
    ( 6,'www.inprice.io/006', '006', 'inprice', 1.00, 'SOCKET_ERROR', 1, 1, 1),
    ( 7,'www.inprice.io/007', '007', 'inprice', 1.00, 'NETWORK_ERROR', 1, 1, 1),
    ( 8,'www.inprice.io/008', '008', 'inprice', 1.00, 'CLASS_PROBLEM', 1, 1, 1),
    ( 9,'www.inprice.io/009', '009', 'inprice', 1.00, 'INTERNAL_ERROR', 1, 1, 1),
    (10,'www.inprice.io/010', '010', 'inprice', 1.00, 'AVAILABLE', 1, 1, 1),
    (11,'www.inprice.io/011', '011', 'inprice', 1.00, 'NEW', 1, 1, 1),

-- UNSUITABLE statuses
    (12,'www.inprice.io/012', '012', 'inprice', 1.00, 'RESUMED', 1, 1, 1),
    (13,'www.inprice.io/013', '013', 'inprice', 1.00, 'PAUSED', 1, 1, 1),
    (14,'www.inprice.io/014', '014', 'inprice', 1.00, 'IMPROPER', 1, 1, 1),
    (15,'www.inprice.io/015', '015', 'inprice', 1.00, 'WONT_BE_IMPLEMENTED', 1, 1, 1),
    (16,'www.inprice.io/016', '016', 'inprice', 1.00, 'DUPLICATE', 1, 1, 1),
    (17,'www.inprice.io/017', '017', 'inprice', 1.00, 'NOT_A_PRODUCT_PAGE', 1, 1, 1),
    (18,'www.inprice.io/018', '018', 'inprice', 1.00, 'NO_DATA', 1, 1, 1),


-- for RESUMED status

-- SUITABLE statuses
    (19,'www.inprice.io/019', '019', 'inprice', 1.00, 'PAUSED', 1, 1, 1),

-- UNSUITABLE statuses
    (20,'www.inprice.io/020', '020', 'inprice', 1.00, 'RESUMED', 1, 1, 1),
    (21,'www.inprice.io/021', '021', 'inprice', 1.00, 'NEW', 1, 1, 1),
    (22,'www.inprice.io/022', '022', 'inprice', 1.00, 'RENEWED', 1, 1, 1),
    (23,'www.inprice.io/023', '023', 'inprice', 1.00, 'BE_IMPLEMENTED', 1, 1, 1),
    (24,'www.inprice.io/024', '024', 'inprice', 1.00, 'IMPLEMENTED', 1, 1, 1),
    (25,'www.inprice.io/025', '025', 'inprice', 1.00, 'NOT_AVAILABLE', 1, 1, 1),
    (26,'www.inprice.io/026', '026', 'inprice', 1.00, 'READ_ERROR', 1, 1, 1),
    (27,'www.inprice.io/027', '027', 'inprice', 1.00, 'SOCKET_ERROR', 1, 1, 1),
    (28,'www.inprice.io/028', '028', 'inprice', 1.00, 'NETWORK_ERROR', 1, 1, 1),
    (29,'www.inprice.io/029', '029', 'inprice', 1.00, 'CLASS_PROBLEM', 1, 1, 1),
    (30,'www.inprice.io/030', '030', 'inprice', 1.00, 'INTERNAL_ERROR', 1, 1, 1),
    (31,'www.inprice.io/031', '031', 'inprice', 1.00, 'IMPROPER', 1, 1, 1),
    (32,'www.inprice.io/032', '032', 'inprice', 1.00, 'DUPLICATE', 1, 1, 1),
    (33,'www.inprice.io/033', '033', 'inprice', 1.00, 'WONT_BE_IMPLEMENTED', 1, 1, 1),
    (34,'www.inprice.io/034', '034', 'inprice', 1.00, 'NOT_A_PRODUCT_PAGE', 1, 1, 1),
    (35,'www.inprice.io/035', '035', 'inprice', 1.00, 'NO_DATA', 1, 1, 1),
    (36,'www.inprice.io/036', '036', 'inprice', 1.00, 'AVAILABLE', 1, 1, 1),

-- for RENEWED status

-- SUITABLE statuses
    (37,'www.inprice.io/037', '037', 'inprice', 1.00, 'AVAILABLE', 1, 1, 1),

-- UNSUITABLE statuses
    (38,'www.inprice.io/038', '038', 'inprice', 1.00, 'RENEWED', 1, 1, 1),
    (39,'www.inprice.io/039', '039', 'inprice', 1.00, 'PAUSED', 1, 1, 1),
    (40,'www.inprice.io/040', '040', 'inprice', 1.00, 'NEW', 1, 1, 1),
    (41,'www.inprice.io/041', '041', 'inprice', 1.00, 'BE_IMPLEMENTED', 1, 1, 1),
    (42,'www.inprice.io/042', '042', 'inprice', 1.00, 'IMPLEMENTED', 1, 1, 1),
    (43,'www.inprice.io/043', '043', 'inprice', 1.00, 'NOT_AVAILABLE', 1, 1, 1),
    (44,'www.inprice.io/044', '044', 'inprice', 1.00, 'READ_ERROR', 1, 1, 1),
    (45,'www.inprice.io/045', '045', 'inprice', 1.00, 'SOCKET_ERROR', 1, 1, 1),
    (46,'www.inprice.io/046', '046', 'inprice', 1.00, 'NETWORK_ERROR', 1, 1, 1),
    (47,'www.inprice.io/047', '047', 'inprice', 1.00, 'CLASS_PROBLEM', 1, 1, 1),
    (48,'www.inprice.io/048', '048', 'inprice', 1.00, 'INTERNAL_ERROR', 1, 1, 1),
    (49,'www.inprice.io/049', '049', 'inprice', 1.00, 'IMPROPER', 1, 1, 1),
    (50,'www.inprice.io/050', '050', 'inprice', 1.00, 'DUPLICATE', 1, 1, 1),
    (51,'www.inprice.io/051', '051', 'inprice', 1.00, 'WONT_BE_IMPLEMENTED', 1, 1, 1),
    (52,'www.inprice.io/052', '052', 'inprice', 1.00, 'NOT_A_PRODUCT_PAGE', 1, 1, 1),
    (53,'www.inprice.io/053', '053', 'inprice', 1.00, 'NO_DATA', 1, 1, 1),
    (54,'www.inprice.io/054', '054', 'inprice', 1.00, 'RESUMED', 1, 1, 1)
    ;
