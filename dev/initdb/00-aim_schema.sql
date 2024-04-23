CREATE TABLE public.modifier (
    id smallint NOT NULL,
    description character varying(200),
    priority bigint DEFAULT 0,
    CONSTRAINT modi_priority_nn CHECK ((priority IS NOT NULL))
);

CREATE TABLE public.sex (
    id character(1) NOT NULL,
    description character varying(200)
);

CREATE TABLE public.patient (
    id bigint NOT NULL,
    sex_fk character(1),
    patient_invalidated_by_fk bigint,
    patient_id character varying(64) NOT NULL,
    last_name character varying(100),
    first_name character varying(100) DEFAULT ''::character varying,
    title character varying(50),
    birth_date date,
    status_deleted smallint NOT NULL,
    inserted_when timestamp without time zone NOT NULL,
    inserted_by_fk smallint NOT NULL,
    last_modified_when timestamp without time zone NOT NULL,
    last_modified_by_fk smallint NOT NULL,
    deleted_when timestamp without time zone,
    deleted_by_fk smallint,
    num_dicom bigint DEFAULT 0,
    num_generics bigint DEFAULT 0,
    vip_indicator_fk character(2) DEFAULT '-'::bpchar,
    CONSTRAINT pat_id_not_empty CHECK (((patient_id)::text <> ''::text)),
    CONSTRAINT pat_ins_by_fk_nn CHECK ((inserted_by_fk IS NOT NULL)),
    CONSTRAINT pat_ins_when_fk_nn CHECK ((inserted_when IS NOT NULL)),
    CONSTRAINT pat_last_mod_by_fk_nn CHECK ((last_modified_by_fk IS NOT NULL)),
    CONSTRAINT pat_last_mod_when_fk_nn CHECK ((last_modified_when IS NOT NULL)),
    CONSTRAINT pat_num_dicom_nn CHECK ((num_dicom IS NOT NULL)),
    CONSTRAINT pat_num_generics_nn CHECK ((num_generics IS NOT NULL)),
    CONSTRAINT pat_patient_id_nn CHECK ((patient_id IS NOT NULL)),
    CONSTRAINT pat_sex_nn CHECK ((sex_fk IS NOT NULL)),
    CONSTRAINT pat_status_deleted_nn CHECK ((status_deleted IS NOT NULL)),
    CONSTRAINT pat_vip_ind_nn CHECK ((vip_indicator_fk IS NOT NULL)),
    CONSTRAINT patient_first_name_nn CHECK ((first_name IS NOT NULL)),
    CONSTRAINT patient_last_name_nn CHECK ((last_name IS NOT NULL))
);

CREATE TABLE public.vip_indicator (
    id character(2) NOT NULL,
    description character varying(200)
);