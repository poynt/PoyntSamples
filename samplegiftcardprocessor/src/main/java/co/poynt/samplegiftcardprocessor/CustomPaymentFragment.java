package co.poynt.samplegiftcardprocessor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.samplegiftcardprocessor.core.TransactionManager;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CustomPaymentFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CustomPaymentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomPaymentFragment extends DialogFragment {
    private static final String ARG_TRANSACTION = "transaction";
    private Transaction transaction;
    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param transaction Transaction.
     * @return A new instance of fragment ZipCodeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CustomPaymentFragment newInstance(Transaction transaction) {
        CustomPaymentFragment fragment = new CustomPaymentFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRANSACTION, transaction);
        fragment.setArguments(args);
        return fragment;
    }

    public CustomPaymentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transaction = getArguments().getParcelable(ARG_TRANSACTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_zip_code, container, false);
        final EditText customPaymentCode = (EditText) view.findViewById(R.id.customPaymentCode);

        Button enterButton = (Button) view.findViewById(R.id.submitButton);
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransactionManager transactionManager =
                        SampleGiftcardTransactionProcessorApplication.getInstance().getTransactionManager();
                Transaction processedTransaction =
                        transactionManager.processTransaction(transaction,
                                customPaymentCode.getText().toString());
                mListener.onFragmentInteraction(processedTransaction, null);
            }
        });

        Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PoyntError error = new PoyntError();
                error.setCode(PoyntError.CARD_DECLINE);
                transaction.setStatus(TransactionStatus.DECLINED);
                mListener.onFragmentInteraction(transaction, error);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Transaction transaction, PoyntError error);
    }

}
